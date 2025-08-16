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

    // 權限請求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.setPermissionsGranted(allGranted)
        
        if (allGranted) {
            setupCamera()
        } else {
            Toast.makeText(this, getString(R.string.error_permissions), Toast.LENGTH_LONG).show()
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

        binding.interruptButton.setOnClickListener {
            viewModel.interruptSpeaking()
        }

        binding.testVoiceButton.setOnClickListener {
            viewModel.startManualVoiceInput()
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
//        lifecycleScope.launch {
//            viewModel.faceDetectionResult.collectLatest { result ->
//                result?.let {
//                    binding.faceCountText.text = getString(R.string.face_detected, it.facesDetected)
//                    binding.faceConfidenceText.text = getString(R.string.face_confidence, it.largestFaceConfidence)
//                }
//            }
//        }

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

        if (permissionsToRequest.isEmpty()) {
            viewModel.setPermissionsGranted(true)
            setupCamera()
        } else {
            requestPermissionLauncher.launch(requiredPermissions)
        }
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
//                setAnalyzer(cameraExecutor) { imageProxy ->
//                    viewModel.processCameraFrame(imageProxy)
//                }
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
