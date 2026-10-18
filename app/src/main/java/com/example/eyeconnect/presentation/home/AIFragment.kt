package com.example.eyeconnect.presentation.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.eyeconnect.databinding.FragmentAiBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AIFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentAiBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // TextToSpeech và trạng thái âm thanh
    private var textToSpeech: TextToSpeech? = null
    private var isSpeakerEnabled = true

    // ML Kit Text Recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Cần cấp quyền Camera để sử dụng", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Khởi tạo công cụ đọc giọng nói tiếng Việt
        textToSpeech = TextToSpeech(requireContext(), this)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupListeners()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("vi", "VN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback nếu máy thiếu gói ngôn ngữ tiếng Việt
                textToSpeech?.setLanguage(Locale.getDefault())
            }
        }
    }

    private fun setupListeners() {
        with(binding) {
            // Nút Loa: Bật/Tắt âm thanh
            ivSpeaker.setOnClickListener {
                vibrateDevice(40)
                isSpeakerEnabled = !isSpeakerEnabled
                if (isSpeakerEnabled) {
                    ivSpeaker.alpha = 1.0f
                    speakText("Đã bật âm thanh")
                } else {
                    ivSpeaker.alpha = 0.4f
                    textToSpeech?.stop()
                    Toast.makeText(requireContext(), "Đã tắt âm thanh", Toast.LENGTH_SHORT).show()
                }
            }

            // Nút Chụp ảnh: Chụp -> Đọc chữ -> Phát âm -> Rung
            btnCapture.setOnClickListener {
                vibrateDevice(60) // Rung 1 nhịp khi người dùng chạm nút chụp
                speakText("Đang xử lý hình ảnh")
                takePhoto()
            }

            // Nút Lịch sử
            btnHistory.setOnClickListener {
                vibrateDevice(40)
                Toast.makeText(requireContext(), "Mở màn hình Lịch sử", Toast.LENGTH_SHORT).show()
            }

            // Nút đóng gợi ý mẹo
            ivCloseSiri.setOnClickListener {
                vibrateDevice(30)
                layoutSiriSuggestion.visibility = View.GONE
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e("AIFragment", "Lỗi khởi động camera: ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return

        val photoFile = File(
            requireContext().cacheDir,
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("AIFragment", "Chụp ảnh thất bại: ${exc.message}")
                    speakText("Chụp ảnh thất bại, vui lòng thử lại")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Sau khi lưu ảnh tạm, chuyển sang bước nhận diện OCR
                    processTextRecognition(photoFile)
                }
            }
        )
    }

    private fun processTextRecognition(photoFile: File) {
        val image = InputImage.fromFilePath(requireContext(), Uri.fromFile(photoFile))

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedText = visionText.text.trim()

                if (detectedText.isNotEmpty()) {
                    // Rung 1 nhịp 150ms báo hiệu nhận diện thành công
                    vibrateDevice(150)
                    // Phát giọng nói đọc văn bản
                    speakText(detectedText)
                    Toast.makeText(requireContext(), detectedText, Toast.LENGTH_LONG).show()
                } else {
                    vibrateDevice(50)
                    speakText("Không tìm thấy văn bản trong hình")
                }

                // Xóa file ảnh tạm sau khi xử lý xong
                photoFile.delete()
            }
            .addOnFailureListener { e ->
                Log.e("AIFragment", "Lỗi nhận diện văn bản: ${e.message}")
                speakText("Không thể nhận diện văn bản")
                photoFile.delete()
            }
    }

    private fun speakText(text: String) {
        if (!isSpeakerEnabled) return
        textToSpeech?.stop()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    private fun vibrateDevice(durationMs: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        _binding = null
        cameraExecutor.shutdown()
    }
}