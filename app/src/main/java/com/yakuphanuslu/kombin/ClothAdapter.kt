package com.yakuphanuslu.kombin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ClothAdapter(
    private val clothList: List<Cloth>,
    private val onItemClick: (Cloth) -> Unit // Tıklama eklendi
) : RecyclerView.Adapter<ClothAdapter.ClothViewHolder>() {

    class ClothViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivClothThumbnail)
        val desc: TextView = view.findViewById(R.id.tvClothDesc)
        val color: TextView = view.findViewById(R.id.tvClothColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cloth, parent, false)
        return ClothViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClothViewHolder, position: Int) {
        val cloth = clothList[position]
        holder.desc.text = cloth.ai_description
        holder.color.text = cloth.color

        Glide.with(holder.itemView.context)
            .load(cloth.image_url)
            .into(holder.image)

        // Tıklama olayını burada ateşliyoruz
        holder.itemView.setOnClickListener {
            onItemClick(cloth)
        }
    }

    override fun getItemCount() = clothList.size
}