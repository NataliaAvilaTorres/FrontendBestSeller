package com.example.bestsellerfrontend

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ReconocimientoFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_reconocimiento_tabs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
        val adapter = PagerAdapter(this)
        viewPager.adapter = adapter

        val btnRegresar: ImageView? = view.findViewById(R.id.btnRegresar)
        btnRegresar?.setOnClickListener { parentFragmentManager.popBackStack() }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "📷 Marca del Producto"
                1 -> "📋 Tabla Nutricional"
                else -> ""
            }
        }.attach()
    }

    inner class PagerAdapter(fragment: Fragment) :
        androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {
        override fun getItemCount() = 2
        override fun createFragment(position: Int): Fragment =
            if (position == 0) MarcaFragment() else NutricionFragment()
    }
}

class MarcaFragment : Fragment() {

    private lateinit var btnSelect: Button
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private lateinit var buttonsContainer: LinearLayout

    private var interpreter: Interpreter? = null
    private lateinit var labels: List<String>
    private var bitmap: Bitmap? = null

    private val PICK_IMAGE_REQUEST = 1
    private val IMG_SIZE = 224

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_marca, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSelect = view.findViewById(R.id.btnSelect)
        imageView = view.findViewById(R.id.imageView)
        textView = view.findViewById(R.id.textView)
        buttonsContainer = view.findViewById(R.id.containerButtons)

        loadModel()
        loadLabels()

        btnSelect.setOnClickListener { selectImage() }
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile("model_unquant.tflite")
            interpreter = Interpreter(modelBuffer)
            textView.text = "✅ Modelo cargado"
        } catch (e: Exception) {
            textView.text = "❌ Error modelo: ${e.message}"
        }
    }

    private fun loadLabels() {
        try {
            val inputStream = requireContext().assets.open("labels.txt")
            val reader = BufferedReader(inputStream.bufferedReader())
            labels = reader.readLines().map { it.trim() }
            reader.close()
            inputStream.close()
        } catch (e: Exception) {
            textView.text = "❌ Error labels: ${e.message}"
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
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap!!, IMG_SIZE, IMG_SIZE, true)

            val inputBuffer = ByteBuffer.allocateDirect(4 * IMG_SIZE * IMG_SIZE * 3)
            inputBuffer.order(ByteOrder.nativeOrder())

            val intValues = IntArray(IMG_SIZE * IMG_SIZE)
            resizedBitmap.getPixels(intValues, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE)
            for (i in intValues.indices) {
                val pixel = intValues[i]
                inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
                inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
                inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
            }
            inputBuffer.rewind()

            val outputArray = Array(1) { FloatArray(labels.size) }
            interpreter!!.run(inputBuffer, outputArray)

            val output = outputArray[0]
            val results = output.withIndex().sortedByDescending { it.value }.take(3)

            // Título de la sección
            textView.text = "🎯 Resultados (elige una marca):"
            // Limpiamos y creamos 3 botones
            buttonsContainer.removeAllViews()

            results.forEachIndexed { i, pred ->
                val label = if (pred.index < labels.size) labels[pred.index] else "Desconocido"
                val confianza = (pred.value * 100).toInt()

                val btn = Button(requireContext()).apply {
                    text = "${i + 1}. $label  (${confianza}%)"
                    isAllCaps = false
                    textSize = 16f
                    setPadding(24, 24, 24, 24)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 12 }
                    setOnClickListener { irAListaProductosConMarca(label) }
                }
                buttonsContainer.addView(btn)
            }

        } catch (e: Exception) {
            textView.text = "❌ Error: ${e.message}"
        }
    }

    private fun irAListaProductosConMarca(marca: String) {
        // 🔹 Elimina el número inicial y espacios, por ejemplo: "0 OMI" -> "OMI"
        val marcaLimpia = marca.replaceFirst(Regex("^\\d+\\s*"), "")

        val frag = ListaProductosFragment().apply {
            arguments = Bundle().apply {
                putString("query_inicial", marcaLimpia)
            }
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, frag)
            .addToBackStack(null)
            .commit()
    }




    override fun onDestroy() {
        super.onDestroy()
        interpreter?.close()
    }
}

class NutricionFragment : Fragment() {

    private lateinit var btnSelect: Button
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private var bitmap: Bitmap? = null
    private val PICK_IMAGE_REQUEST = 2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_nutricion, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnSelect = view.findViewById(R.id.btnSelect)
        imageView = view.findViewById(R.id.imageView)
        textView = view.findViewById(R.id.textView)

        textView.text = "Selecciona la foto de la tabla nutricional"
        btnSelect.setOnClickListener { selectImage() }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data ?: return
            bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            imageView.setImageBitmap(bitmap)
            extractAndAnalyzeNutrition()
        }
    }

    private fun extractAndAnalyzeNutrition() {
        if (bitmap == null) {
            textView.text = "❌ Selecciona una imagen"
            return
        }

        textView.text = "📋 Enviando imagen al backend...\n(Por favor espera...)"

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                sendImageToBackend(bitmap!!)
            }
            textView.text = result
        }
    }

    private fun sendImageToBackend(bitmap: Bitmap): String {
        return try {
            val file = File(requireContext().cacheDir, "nutrition_table.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            val requestBody = file.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.name, requestBody)
                .build()

            val request = Request.Builder()
                //.url("http://10.0.2.2:8090/api/nutricion/analizar")
                .url("http://192.168.1.13:8090/api/nutricion/analizar")
                .post(multipartBody)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                formatAnalysisResponse(JSONObject(responseBody))
            } else {
                "⚠️ Error ${response.code}: ${response.message}"
            }

        } catch (e: Exception) {
            "❌ Error al conectar con backend: ${e.message}"
        }
    }

    private fun formatAnalysisResponse(json: JSONObject): String {
        val analysis = StringBuilder("📊 ANÁLISIS NUTRICIONAL DETALLADO:\n\n")

        try {
            if (json.has("nutrientes")) {
                val nutrientes = json.getJSONObject("nutrientes")
                analysis.append("1️⃣ NUTRIENTES DETECTADOS:\n")
                nutrientes.keys().forEach { key ->
                    val valor = nutrientes.getInt(key)
                    if (valor > 0) analysis.append("   ✅ ${key.uppercase()}: $valor\n")
                }
            }

            if (json.has("evaluacion")) {
                analysis.append("\n2️⃣ EVALUACIÓN:\n   ${json.getString("evaluacion")}\n")
            }

            if (json.has("recomendaciones")) {
                val recomendaciones = json.getJSONArray("recomendaciones")
                analysis.append("\n3️⃣ RECOMENDACIONES:\n")
                for (i in 0 until recomendaciones.length()) {
                    analysis.append("   • ${recomendaciones.getString(i)}\n")
                }
            }

        } catch (e: Exception) {
            analysis.append("\n⚠️ Error interpretando respuesta: ${e.message}\n")
        }

        return analysis.toString()
    }
}
