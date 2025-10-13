package com.example.bestsellerfrontend

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class ReconocimientoActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private val PICK_IMAGE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reconocimiento)

        imageView = findViewById(R.id.imageView)
        textView = findViewById(R.id.textView)
        val btnSelect = findViewById<Button>(R.id.btnSelect)

        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            imageUri?.let {
                imageView.setImageURI(it)
                analizarImagen(it)
            }
        }
    }

    private fun analizarImagen(uri: Uri) {
        val image = InputImage.fromFilePath(this, uri)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                val resultado = StringBuilder()
                for (label in labels) {
                    val text = label.text
                    val confidence = label.confidence
                    resultado.append("$text (${String.format("%.2f", confidence * 100)}%)\n")
                }
                textView.text = resultado.toString()
            }
            .addOnFailureListener { e ->
                textView.text = "Error: ${e.message}"
            }
    }
}