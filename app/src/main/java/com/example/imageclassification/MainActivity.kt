package com.example.imageclassification

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.imageclassification.databinding.ActivityMainBinding
import com.example.imageclassification.ml.MobilenetV110224Quant
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.util.*
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.os.bundleOf
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var bitmap: Bitmap ?= null
    private val CAPTURE_IMAGE_REQUEST = 1
    private val SELECT_IMAGE_REQUEST = 2
    private lateinit var textToSpeech: TextToSpeech
    private val resultsList = ArrayList<Pair<File, File>>()
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            binding.camera.performClick()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.camera.setOnClickListener{
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAPTURE_IMAGE_REQUEST)
        }

        binding.btnSelectImage.setOnClickListener{
            var intent = Intent()
            intent.setAction(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startActivityForResult(intent, SELECT_IMAGE_REQUEST)
        }
        binding.btnSave.setOnClickListener{
            if (bitmap != null && binding.textView.text.isNotEmpty()) {
                val timestamp = System.currentTimeMillis()
                val imageFile = File(this.filesDir, "imageFile_$timestamp")
                val fos = FileOutputStream(imageFile)
                bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()

                val textFile = File(this.filesDir, "textFile_$timestamp")
                textFile.writeText(binding.textView.text.toString())

                resultsList.add(Pair(imageFile, textFile))
                Toast.makeText(this, "Result saved", Toast.LENGTH_SHORT).show()
                handler.postDelayed(runnable, 5000)
            } else {
                Toast.makeText(this, "No result to save", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnViewSaved.setOnClickListener {
            val intent = Intent(this, SavedImagesActivity::class.java)
            intent.putExtra("results", resultsList)
            startActivity(intent)
        }


        val labels = application.assets.open("labels.txt").bufferedReader().readLines()

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224,224, ResizeOp.ResizeMethod.BILINEAR)).build()

        textToSpeech = TextToSpeech(applicationContext) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech.language = Locale.US
            }
        }
        binding.btnPredict.setOnClickListener{
            if(bitmap != null) {
                val tensorImage = TensorImage(DataType.UINT8)
                tensorImage.load(bitmap)
                val processedImage = imageProcessor.process(tensorImage)

                val model = MobilenetV110224Quant.newInstance(this)

                val inputFeature0 =
                    TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.UINT8)
                inputFeature0.loadBuffer(processedImage.buffer)

                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

                var maxIdx = outputFeature0.indices.maxByOrNull { outputFeature0[it] } ?: -1

                val prediction = labels[maxIdx]

                binding.textView.setText(labels[maxIdx])

                textToSpeech.speak(prediction, TextToSpeech.QUEUE_FLUSH, null, "")
                model.close()

            }else {
                // If there is no image, display a message
                Toast.makeText(this, "No image to predict", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAPTURE_IMAGE_REQUEST -> {
                    bitmap = data?.extras?.get("data") as Bitmap
                }
                SELECT_IMAGE_REQUEST -> {
                    val selectedImageUri = data?.data
                    bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImageUri)
                }
            }
            binding.ivImage.setImageBitmap(bitmap)
        }
    }

    override fun onDestroy() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        textToSpeech.shutdown()
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}