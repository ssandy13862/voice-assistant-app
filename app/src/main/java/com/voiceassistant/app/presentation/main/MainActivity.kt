package com.voiceassistant.app.presentation.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.voiceassistant.app.R
import com.voiceassistant.app.databinding.ActivityMainBinding
import com.voiceassistant.app.domain.model.VoiceAssistantState
import com.voiceassistant.app.presentation.adapter.ConversationAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 主Activity - 語音助理介面
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    
    private lateinit var conversationAdapter: ConversationAdapter
    
    // 人臉檢測頻率控制
    private var lastFaceDetectionTime = 0L
    private val faceDetectionInterval = 500L // 每500ms檢測一次（2fps）

    // 權限請求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.setPermissionsGranted(allGranted)
        
        if (allGranted) {
            android.util.Log.i("MainActivity", "所有權限已授予")
            setupCamera()
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            android.util.Log.w("MainActivity", "權限被拒絕: $deniedPermissions")
            
            val message = if (deniedPermissions.contains(Manifest.permission.RECORD_AUDIO)) {
                "需要麥克風權限才能使用語音功能"
            } else {
                getString(R.string.error_permissions)
            }
            
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            
            // 顯示權限說明對話框
            showPermissionExplanationDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setupViews()
        setupObservers()
        checkPermissions()
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupViews() {
        // 設置對話歷史RecyclerView
        conversationAdapter = ConversationAdapter()
        binding.conversationRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = conversationAdapter
        }

        // 設置按鈕點擊事件
        binding.freeModeButton.setOnClickListener {
            viewModel.toggleFreeMode()
        }

        binding.clearHistoryButton.setOnClickListener {
            viewModel.clearConversationHistory()
        }
        
        // 長按清除按鈕檢查權限
        binding.clearHistoryButton.setOnLongClickListener {
            checkAndShowPermissionStatus()
            true
        }

        binding.interruptButton.setOnClickListener {
            viewModel.interruptSpeaking()
        }

        binding.testVoiceButton.setOnClickListener {
            viewModel.testWhisperSTT()
        }
        
        // 長按執行 Silero VAD 測試
        binding.testVoiceButton.setOnLongClickListener {
            viewModel.testSileroVad()
            true
        }
    }

    private fun setupObservers() {
        // 觀察語音助理狀態
        lifecycleScope.launch {
            viewModel.assistantState.collectLatest { state ->
                updateStateUI(state)
            }
        }

        // 觀察對話歷史
        lifecycleScope.launch {
            viewModel.conversationHistory.collectLatest { conversations ->
                conversationAdapter.submitList(conversations)
                if (conversations.isNotEmpty()) {
                    binding.conversationRecyclerView.smoothScrollToPosition(conversations.size - 1)
                }
            }
        }

        // 觀察人臉檢測結果
        lifecycleScope.launch {
            viewModel.faceDetectionResult.collectLatest { result ->
                result?.let {
                    binding.faceCountText.text = "檢測到 ${it.facesDetected} 張臉孔"
                    binding.faceConfidenceText.text = "最大置信度: ${String.format("%.2f", it.largestFaceConfidence)}"
                } ?: run {
                    binding.faceCountText.text = "無人臉檢測"
                    binding.faceConfidenceText.text = "置信度: N/A"
                }
            }
        }

        // 自由模式狀態
        lifecycleScope.launch {
            viewModel.isFreeMode.collectLatest { isFreeMode ->
                binding.freeModeButton.text = if (isFreeMode) {
                    getString(R.string.exit_free_mode)
                } else {
                    getString(R.string.enter_free_mode)
                }
                binding.freeModeIndicator.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        if (isFreeMode) android.R.color.holo_green_light else android.R.color.darker_gray
                    )
                )
            }
        }

        // 錯誤狀態
        lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { error ->
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_LONG).show()
            }
        }
        
        // 模型下載失敗對話框
        lifecycleScope.launch {
            viewModel.modelDownloadFailed.collectLatest { message ->
                showModelDownloadFailedDialog(message)
            }
        }

        // 權限狀態
        lifecycleScope.launch {
            viewModel.permissionsGranted.collectLatest { granted ->
                binding.permissionStatus.text = if (granted) {
                    getString(R.string.permissions_granted)
                } else {
                    getString(R.string.permissions_needed)
                }
                binding.permissionStatus.setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        if (granted) android.R.color.holo_green_dark else android.R.color.holo_red_dark
                    )
                )
            }
        }
    }

    private fun updateStateUI(state: VoiceAssistantState) {
        binding.stateText.text = viewModel.getStateDisplayText(state)
        binding.stateIndicator.setBackgroundColor(
            ContextCompat.getColor(this, viewModel.getStateColor(state))
        )

        // 根據狀態更新 UI
        binding.interruptButton.isEnabled = (state == VoiceAssistantState.SPEAKING)
    }

    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        android.util.Log.i("MainActivity", "檢查權限，需要請求: $permissionsToRequest")

        if (permissionsToRequest.isEmpty()) {
            android.util.Log.i("MainActivity", "所有權限已授予")
            viewModel.setPermissionsGranted(true)
            setupCamera()
        } else {
            android.util.Log.i("MainActivity", "請求權限: $permissionsToRequest")
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }
    
    /**
     * 顯示權限說明對話框
     */
    private fun showPermissionExplanationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("權限請求")
            .setMessage("本應用需要以下權限才能正常運作\uFFFD\n\n• 相機權限：用於人臉識別\n• 麥克風權限：用於語音識別\n\n是否重新請求權限？")
            .setPositiveButton("重新請求") { _, _ ->
                checkPermissions()
            }
            .setNegativeButton("取消") { _, _ ->
                Toast.makeText(this, "無法使用應用功能", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 檢查並顯示權限狀態
     */
    private fun checkAndShowPermissionStatus() {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        
        val cameraGranted = cameraPermission == PackageManager.PERMISSION_GRANTED
        val audioGranted = audioPermission == PackageManager.PERMISSION_GRANTED
        
        val message = """
            權限狀態：
            • 相機權限：${if (cameraGranted) "✅ 已授予" else "❌ 未授予"}
            • 麥克風權限：${if (audioGranted) "✅ 已授予" else "❌ 未授予"}
        """.trimIndent()
        
        android.util.Log.i("MainActivity", "權限狀態 - 相機: $cameraGranted, 麥克風: $audioGranted")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("權限狀態")
            .setMessage(message)
            .setPositiveButton("確定") { _, _ -> }
            .setNeutralButton("重新請求") { _, _ ->
                if (!cameraGranted || !audioGranted) {
                    checkPermissions()
                }
            }
            .show()
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        // Preview用例
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(binding.cameraPreview.surfaceProvider)
        }

        // ImageAnalysis用例用於人臉檢測
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(cameraExecutor) { imageProxy ->
                    val currentTime = System.currentTimeMillis()
                    
                    // 控制人臉檢測頻率，減少 ML Kit 日誌輸出
                    if (currentTime - lastFaceDetectionTime >= faceDetectionInterval) {
                        lastFaceDetectionTime = currentTime
                        viewModel.processCameraFrame(imageProxy)
                    } else {
                        imageProxy.close() // 關閉跳過的幀
                    }
                }
            }

        // 選擇前置攝影機
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            // 解綁之前的用例
            cameraProvider.unbindAll()

            // 綁定用例到生命週期
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Toast.makeText(this, getString(R.string.error_camera_start, exc.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 顯示模型下載失敗對話框
     */
    private fun showModelDownloadFailedDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ VAD 模型下載失敗")
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("繼續使用") { _, _ ->
                // 用戶選擇繼續使用簡化功能
                Toast.makeText(this, "將使用簡化語音檢測功能", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("重試下載") { _, _ ->
                // 重新嘗試初始化 VAD
                viewModel.retryVadInitialization()
            }
            .setNeutralButton("檢查網路") { _, _ ->
                // 提示用戶檢查網路設置
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("網路連線檢查")
                    .setMessage("""
                        請確認：
                        • Wi-Fi 或行動網路已連接
                        • 網路連線穩定
                        • 防火牆未阻擋應用程式
                        • 嘗試重新啟動應用程式
                    """.trimIndent())
                    .setPositiveButton("確定") { _, _ -> }
                    .show()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
