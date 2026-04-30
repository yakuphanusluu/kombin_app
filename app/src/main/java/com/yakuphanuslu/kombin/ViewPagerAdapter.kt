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
        // Sekme sırasını veritabanındaki kategori ID'leri ile eşliyoruz
        val categoryId = when(position) {
            0 -> 1 // Üst Giyim
            1 -> 2 // Alt Giyim
            2 -> 4 // Ayakkabı (Veritabanında ID'si 4 olduğu için direkt 4'e bağlıyoruz)
            else -> 1
        }

        // Tıklama fonksiyonunu ve doğru kategori ID'sini fragment'a paslıyoruz
        return CategoryFragment(categoryId, clothes, onItemClick)
    }
}