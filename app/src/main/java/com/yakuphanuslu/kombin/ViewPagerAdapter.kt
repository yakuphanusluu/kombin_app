package com.yakuphanuslu.kombin

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    activity: AppCompatActivity,
    private val clothes: List<Cloth>,
    private val onItemClick: (Cloth) -> Unit // Tıklama eklendi
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        // Tıklama fonksiyonunu fragment'a paslıyoruz
        return CategoryFragment(position + 1, clothes, onItemClick)
    }
}