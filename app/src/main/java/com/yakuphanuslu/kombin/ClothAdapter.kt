package com.yakuphanuslu.kombin

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ClothAdapter(
    private val clothList: MutableList<Cloth>, // List yerine MutableList yaptık ki silebilelim
    private val onItemClick: (Cloth) -> Unit
) : RecyclerView.Adapter<ClothAdapter.ClothViewHolder>() {

    class ClothViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivClothThumbnail)
        val desc: TextView = view.findViewById(R.id.tvClothDesc)
        val color: TextView = view.findViewById(R.id.tvClothColor)

        // YENİ: Çarpı butonumuzu buraya tanıttık
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteClothing)
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

        // Mevcut yapı: Tüm karta tıklanınca detay falan açılıyorsa o çalışmaya devam eder
        holder.itemView.setOnClickListener {
            onItemClick(cloth)
        }

        // YENİ EKLENEN KISIM: Sadece Çarpı butonuna tıklama olayı
        holder.btnDelete.setOnClickListener {
            val context = holder.itemView.context

            // Yanlışlıkla basmalara karşı emin misin diye soruyoruz
            AlertDialog.Builder(context)
                .setTitle("Kıyafeti Sil")
                .setMessage("Bu kıyafeti dolabından silmek istediğine emin misin?")
                .setPositiveButton("Evet, Sil") { _, _ ->

                    // Kullanıcı ID'sini ve Kıyafet ID'sini alıyoruz
                    val sharedPref = context.getSharedPreferences("KombinApp", Context.MODE_PRIVATE)
                    val userId = sharedPref.getInt("user_id", -1)
                    val clothingId = cloth.id // Cloth modelinde 'id' olduğunu varsayıyoruz

                    if (userId != -1) {
                        // Retrofit ayarları
                        val retrofit = Retrofit.Builder()
                            .baseUrl("https://yakuphanuslu.com/kombin_api/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()

                        val apiService = retrofit.create(ApiService::class.java)

                        // PHP'ye silme isteği atıyoruz
                        apiService.deleteClothing(userId, clothingId).enqueue(object : Callback<LoginResponse> {
                            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                                if (response.isSuccessful && response.body()?.status == "success") {
                                    Toast.makeText(context, "Kıyafet silindi!", Toast.LENGTH_SHORT).show()

                                    // Sildiğimiz elemanı Listeden çıkarıp, RecyclerView'a animasyonlu haber veriyoruz
                                    val currentPosition = holder.adapterPosition
                                    if (currentPosition != RecyclerView.NO_POSITION) {
                                        clothList.removeAt(currentPosition)
                                        notifyItemRemoved(currentPosition)
                                        notifyItemRangeChanged(currentPosition, clothList.size)
                                    }
                                } else {
                                    Toast.makeText(context, "Silinemedi: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                Toast.makeText(context, "Bağlantı hatası!", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
                .setNegativeButton("İptal", null)
                .show()
        }
    }

    override fun getItemCount() = clothList.size
}