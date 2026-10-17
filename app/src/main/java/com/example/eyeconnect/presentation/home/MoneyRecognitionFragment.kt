package com.example.eyeconnect.presentation.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.eyeconnect.databinding.FragmentMoneyRecognitionBinding
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MoneyRecognitionFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentMoneyRecognitionBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var moneyClassifier: MoneyClassifier

    private var textToSpeech: TextToSpeech? = null
    private var isSpeakerEnabled = true

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Cần cấp quyền Camera để nhận diện tiền", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoneyRecognitionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        moneyClassifier = MoneyClassifier(requireContext())
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
                textToSpeech?.setLanguage(Locale.getDefault())
            }
        }
    }

    private fun setupListeners() {
        with(binding) {
            // Nút Bật / Tắt âm thanh giọng đọc
            ivMoneySpeaker.setOnClickListener {
                vibrateDevice(40)
                isSpeakerEnabled = !isSpeakerEnabled
                if (isSpeakerEnabled) {
                    ivMoneySpeaker.alpha = 1.0f
                    speakText("Đã bật âm thanh")
                } else {
                    ivMoneySpeaker.alpha = 0.4f
                    textToSpeech?.stop()
                    Toast.makeText(requireContext(), "Đã tắt âm thanh", Toast.LENGTH_SHORT).show()
                }
            }

            // Nút Chụp ảnh
            btnCaptureMoney.setOnClickListener {
                vibrateDevice(60)
                speakText("Đang nhận diện")
                takePhotoAndClassify()
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
                    it.setSurfaceProvider(binding.moneyPreviewView.surfaceProvider)
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
                Log.e("MoneyRecognition", "Lỗi mở camera: ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhotoAndClassify() {
        val capture = imageCapture ?: return

        capture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("MoneyRecognition", "Chụp ảnh thất bại: ${exc.message}")
                    speakText("Chụp ảnh thất bại, vui lòng thử lại")
                }

                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    cameraExecutor.execute {
                        val bitmap = imageProxyToBitmap(imageProxy)
                        imageProxy.close()

                        if (bitmap != null) {
                            val result = moneyClassifier.classifyBitmap(bitmap)

                            requireActivity().runOnUiThread {
                                vibrateDevice(120)
                                speakText(result.spokenMessage)
                                Toast.makeText(requireContext(), result.spokenMessage, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            requireActivity().runOnUiThread {
                                speakText("Lỗi phân tích hình ảnh")
                            }
                        }
                    }
                }
            }
        )
    }

    // Chuyển ImageProxy thành Bitmap và xoay ảnh về góc thẳng đứng chuẩn xác
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
            }
            Bitmap.createBitmap(
                originalBitmap, 0, 0,
                originalBitmap.width, originalBitmap.height,
                matrix, true
            )
        } else {
            originalBitmap
        }
    }

    private fun speakText(text: String) {
        if (!isSpeakerEnabled || text.isEmpty()) return
        textToSpeech?.stop()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MONEY_TTS_ID")
    }

    private fun vibrateDevice(durationMs: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
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
        moneyClassifier.close()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        _binding = null
        cameraExecutor.shutdown()
    }
}
