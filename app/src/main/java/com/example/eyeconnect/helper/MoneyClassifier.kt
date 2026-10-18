package com.example.eyeconnect.presentation.home

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MoneyClassifier(context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    init {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, "money_model.tflite")
            interpreter = Interpreter(modelBuffer)
            labels = FileUtil.loadLabels(context, "labels.txt")
            Log.d("MoneyClassifier", "Nạp thành công ${labels.size} nhãn: $labels")
        } catch (e: Exception) {
            Log.e("MoneyClassifier", "Lỗi nạp model hoặc labels: ${e.message}")
        }
    }

    data class RecognitionResult(
        val label: String,
        val confidence: Float,
        val spokenMessage: String
    )

    fun classifyBitmap(bitmap: Bitmap): RecognitionResult {
        val tflite =
            interpreter ?: return RecognitionResult("Error", 0f, "Chưa nạp được mô hình AI")

        // 1. Chuẩn hóa ảnh: Resize 224x224 và chia 255.0 để chuẩn hóa về [0.0f, 1.0f]
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0.0f, 255.0f))
            .build()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Chạy suy luận
        val outputBuffer =
            TensorBuffer.createFixedSize(intArrayOf(1, labels.size), DataType.FLOAT32)
        tflite.run(tensorImage.buffer, outputBuffer.buffer.rewind())

        // 3. Tìm nhãn có độ tin cậy cao nhất
        val confidences = outputBuffer.floatArray
        var maxIndex = 0
        var maxConfidence = 0f

        for (i in confidences.indices) {
            Log.d("MoneyClassifier", "Nhãn [${labels.getOrNull(i)}]: ${confidences[i]}")
            if (confidences[i] > maxConfidence) {
                maxConfidence = confidences[i]
                maxIndex = i
            }
        }

        val rawLabel = labels.getOrElse(maxIndex) { "Unknown" }
        // Loại bỏ số thứ tự ở đầu dòng (ví dụ "0 100K" -> "100K")
        val cleanLabel = rawLabel.replace(Regex("^[0-9]+\\s*"), "").trim()

        Log.d("MoneyClassifier", "Kết quả dự đoán: $cleanLabel với độ tin cậy: $maxConfidence")

        // 4. Đặt ngưỡng tối thiểu 50% (0.50f) để dễ nhận diện trong nhiều điều kiện sáng
        return if (maxConfidence >= 0.50f) {
            // Kiểm tra nhãn Background (bắt cả chữ Backgroud bị thiếu chữ n)
            if (cleanLabel.contains("Backgroud", ignoreCase = true)) {
                RecognitionResult(
                    cleanLabel,
                    maxConfidence,
                    "Không nhận diện được tờ tiền nào trong khung"
                )
            } else {
                val formattedMoney = when (cleanLabel.uppercase()) {
                    "1K" -> "Một nghìn"
                    "2K" -> "Hai nghìn"
                    "5K" -> "Năm nghìn"
                    "10K" -> "Mười nghìn"
                    "20K" -> "Hai mươi nghìn"
                    "50K" -> "Năm mươi nghìn"
                    "100K" -> "Một trăm nghìn"
                    "200K" -> "Hai trăm nghìn"
                    "500K" -> "Năm trăm nghìn"
                    else -> cleanLabel
                }
                RecognitionResult(cleanLabel, maxConfidence, "$formattedMoney đồng")
            }
        } else {
            RecognitionResult(
                "LowConfidence",
                maxConfidence,
                "Không rõ mệnh giá, vui lòng đưa tờ tiền phẳng vào khung và chụp lại"
            )
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}