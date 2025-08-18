# Silero VAD 模型下載指南

## 下載模型
請按照以下步驟下載 Silero VAD 模型：

1. 訪問 Silero VAD 官方倉庫：
   https://github.com/snakers4/silero-vad

2. 下載預訓練的 ONNX 模型：
   ```bash
   wget https://github.com/snakers4/silero-vad/raw/master/src/silero_vad/data/silero_vad.onnx
   ```

3. 將下載的 `silero_vad.onnx` 文件放置到：
   `app/src/main/assets/silero_vad.onnx`

## 模型信息
- **模型名稱**: Silero VAD
- **文件大小**: 約 1.4MB
- **輸入格式**: 16kHz 單聲道音頻，512樣本塊
- **輸出**: 語音活動概率 (0.0-1.0)

## 許可證
Silero VAD 使用 MIT 許可證，允許商業使用。

## 替代方案
如果無法下載模型，應用程序會自動降級為簡單的能量檢測 VAD。