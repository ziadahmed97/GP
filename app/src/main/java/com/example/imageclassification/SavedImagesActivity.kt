package com.example.imageclassification

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imageclassification.databinding.ActivitySavedImagesBinding
import java.io.File

class SavedImagesActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySavedImagesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedImagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val resultsList: ArrayList<Pair<File, File>> = intent.getSerializableExtra("results") as ArrayList<Pair<File, File>>

        // Set the layout manager and adapter for the RecyclerView
        binding.RV.layoutManager = LinearLayoutManager(this)
        binding.RV.adapter = ResultsAdapter(resultsList)
    }
}

class ResultsAdapter(private val results: List<Pair<File, File>>) : RecyclerView.Adapter<ResultsAdapter.ResultViewHolder>() {

    class ResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView:ImageView
        val textView: TextView
        init {
            imageView = view.findViewById(R.id.itemImage)
            textView = view.findViewById(R.id.itemText)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val result = results[position]
        val bitmap = BitmapFactory.decodeFile(result.first.path)
        val text = result.second.readText()
        holder.imageView.setImageBitmap(bitmap)
        holder.textView.text = text
    }

    override fun getItemCount() = results.size
}
