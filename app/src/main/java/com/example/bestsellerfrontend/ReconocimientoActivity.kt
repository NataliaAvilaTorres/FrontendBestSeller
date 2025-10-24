package com.example.bestsellerfrontend

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ReconocimientoFragment : Fragment() {

    private lateinit var btnSelect: Button
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private var interpreter: Interpreter? = null
    private lateinit var labels: List<String>
    private var bitmap: Bitmap? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val IMG_SIZE = 224
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_reconocimiento, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSelect = view.findViewById(R.id.btnSelect)
        imageView = view.findViewById(R.id.imageView)
        textView = view.findViewById(R.id.textView)

        val btnRegresar: ImageView? = view.findViewById(R.id.btnRegresar)
        btnRegresar?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadModel()
        loadLabels()

        btnSelect.setOnClickListener {
            selectImage()
        }
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile("model_unquant.tflite")
            interpreter = Interpreter(modelBuffer)
            textView.text = "✅ Modelo cargado"
        } catch (e: Exception) {
            textView.text = "❌ Error modelo: ${e.message}"
            e.printStackTrace()
        }
    }

    private fun loadLabels() {
        try {
            val inputStream = requireContext().assets.open("labels.txt")
            val reader = BufferedReader(inputStream.bufferedReader())
            labels = reader.readLines().map { it.trim() }
            reader.close()
            inputStream.close()
            println("✅ Labels cargados: ${labels.size} clases")
        } catch (e: Exception) {
            textView.text = "❌ Error labels: ${e.message}"
            e.printStackTrace()
        }
    }

    private fun loadModelFile(modelName: String): ByteBuffer {
        val assetManager = requireContext().assets
        val fileDescriptor = assetManager.openFd(modelName)
        val inputStream = fileDescriptor.createInputStream()
        val fileSize = fileDescriptor.declaredLength.toInt()
        val buffer = ByteBuffer.allocateDirect(fileSize)
        buffer.order(ByteOrder.nativeOrder())

        val bytes = ByteArray(fileSize)
        var bytesRead = 0
        while (bytesRead < fileSize) {
            bytesRead += inputStream.read(bytes, bytesRead, fileSize - bytesRead)
        }
        buffer.put(bytes)
        buffer.rewind()

        inputStream.close()
        return buffer
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                    imageView.setImageBitmap(bitmap)
                    detectMarca()
                } catch (e: Exception) {
                    textView.text = "❌ Error cargando imagen"
                }
            }
        }
    }

    private fun detectMarca() {
        if (bitmap == null) {
            textView.text = "❌ Selecciona una imagen"
            return
        }

        if (interpreter == null) {
            textView.text = "❌ Modelo no cargado"
            return
        }

        try {
            textView.text = "🔍 Detectando marca..."

            // Redimensionar
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap!!, IMG_SIZE, IMG_SIZE, true)

            // Convertir a ByteBuffer
            val inputBuffer = ByteBuffer.allocateDirect(4 * IMG_SIZE * IMG_SIZE * 3)
            inputBuffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(IMG_SIZE * IMG_SIZE)
            resizedBitmap.getPixels(intValues, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE)

            for (i in intValues.indices) {
                val `val` = intValues[i]
                inputBuffer.putFloat(((`val` shr 16) and 0xFF) / 255.0f)
                inputBuffer.putFloat(((`val` shr 8) and 0xFF) / 255.0f)
                inputBuffer.putFloat((`val` and 0xFF) / 255.0f)
            }

            inputBuffer.rewind()

            // Predicción
            val outputArray = Array(1) { FloatArray(labels.size) }
            interpreter!!.run(inputBuffer, outputArray)
            val output = outputArray[0]

            // Top 3 resultados
            val results = output.withIndex()
                .sortedByDescending { it.value }
                .take(3)

            val resultText = StringBuilder("🎯 Top 3 Resultados:\n\n")
            for ((index, prediction) in results.withIndex()) {
                val label = if (prediction.index < labels.size) {
                    labels[prediction.index]
                } else {
                    "Desconocido"
                }
                val confianza = (prediction.value * 100).toInt()
                resultText.append("${index + 1}. $label\n   ${confianza}% confianza\n\n")
            }

            textView.text = resultText.toString()

        } catch (e: Exception) {
            textView.text = "❌ Error: ${e.message}"
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        interpreter?.close()
    }
}