package com.yakuphanuslu.kombin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoryFragment(
    private val categoryId: Int,
    private val allClothes: List<Cloth>,
    private val onItemClick: (Cloth) -> Unit // Tıklama eklendi
) : Fragment(R.layout.fragment_category) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.rvCategoryClothes)

        val filteredList = allClothes.filter { it.category_id == categoryId }

        rv.layoutManager = GridLayoutManager(context, 2)

        // BURASI GÜNCELLENDİ: filteredList'i MutableList'e çevirerek gönderiyoruz
        rv.adapter = ClothAdapter(filteredList.toMutableList(), onItemClick)
    }
}