package com.yakuphanuslu.kombin

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.yakuphanuslu.kombin.BuildConfig

// BuildConfig importu otomatik gelmezse manuel ekle kanka
// import com.yakuphanuslu.kombin.BuildConfig

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var allClothesList: List<Cloth> = emptyList()

    private var selectedTop: Cloth? = null
    private var selectedBottom: Cloth? = null
    private var selectedShoes: Cloth? = null

    private lateinit var fabAiGenerate: ExtendedFloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("KombinApp", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        if (userId == -1) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val rootLayout = findViewById<ViewGroup>(R.id.mainRoot)
        rootLayout.layoutTransition?.enableTransitionType(LayoutTransition.CHANGING)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        fabAiGenerate = findViewById(R.id.fabAiGenerate)

        findViewById<ImageButton>(R.id.ivSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddClothingActivity::class.java))
        }

        fabAiGenerate.setOnClickListener { generateOutfitWithAi() }

        findViewById<Button>(R.id.btnAnalyze).setOnClickListener { openKombinReviewPanel() }

        findViewById<ImageButton>(R.id.btnRemoveTop).setOnClickListener { clearSelection(1) }
        findViewById<ImageButton>(R.id.btnRemoveBottom).setOnClickListener { clearSelection(2) }
        findViewById<ImageButton>(R.id.btnRemoveShoes).setOnClickListener { clearSelection(3) }
    }

    override fun onResume() {
        super.onResume()
        getClothesFromServer()
    }

    private fun getClothesFromServer() {
        val sharedPref = getSharedPreferences("KombinApp", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://yakuphanuslu.com/kombin_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        apiService.getClothes(userId).enqueue(object : Callback<List<Cloth>> {
            override fun onResponse(call: Call<List<Cloth>>, response: Response<List<Cloth>>) {
                if (!isFinishing && response.isSuccessful) {
                    allClothesList = response.body() ?: emptyList()
                    viewPager.adapter = ViewPagerAdapter(this@MainActivity, allClothesList) { selected ->
                        handleSelection(selected)
                    }
                    TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                        tab.text = when(position) {
                            0 -> "Üst Giyim"
                            1 -> "Alt Giyim"
                            else -> "Ayakkabı"
                        }
                    }.attach()
                }
            }
            override fun onFailure(call: Call<List<Cloth>>, t: Throwable) {
                if (!isFinishing) Toast.makeText(this@MainActivity, "Veriler çekilemedi kanka!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setBottomSheetWidth(dialog: BottomSheetDialog) {
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels

            if (screenWidth > 1600) {
                val layoutParams = it.layoutParams
                layoutParams.width = (600 * displayMetrics.density).toInt()
                it.layoutParams = layoutParams
            }
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun handleSelection(cloth: Cloth) {
        findViewById<View>(R.id.llKombinPreview).visibility = View.VISIBLE
        when (cloth.category_id) {
            1 -> { selectedTop = cloth; updatePreviewUI(R.id.ivPreviewTop, R.id.btnRemoveTop, cloth.image_url) }
            2 -> { selectedBottom = cloth; updatePreviewUI(R.id.ivPreviewBottom, R.id.btnRemoveBottom, cloth.image_url) }
            3 -> { selectedShoes = cloth; updatePreviewUI(R.id.ivPreviewShoes, R.id.btnRemoveShoes, cloth.image_url) }
        }
    }

    private fun updatePreviewUI(ivId: Int, btnId: Int, url: String?) {
        val iv = findViewById<ImageView>(ivId)
        val btn = findViewById<ImageButton>(btnId)
        Glide.with(this).load(url).into(iv)
        iv.alpha = 1.0f
        btn.visibility = View.VISIBLE
    }

    private fun clearSelection(categoryId: Int) {
        when (categoryId) {
            1 -> { selectedTop = null; resetImageView(R.id.ivPreviewTop, R.id.btnRemoveTop) }
            2 -> { selectedBottom = null; resetImageView(R.id.ivPreviewBottom, R.id.btnRemoveBottom) }
            3 -> { selectedShoes = null; resetImageView(R.id.ivPreviewShoes, R.id.btnRemoveShoes) }
        }
        if (selectedTop == null && selectedBottom == null && selectedShoes == null) {
            findViewById<View>(R.id.llKombinPreview).visibility = View.GONE
        }
    }

    private fun resetImageView(ivId: Int, btnId: Int) {
        val iv = findViewById<ImageView>(ivId)
        val btn = findViewById<ImageButton>(btnId)
        iv.setImageResource(android.R.drawable.ic_menu_gallery)
        iv.alpha = 0.5f
        btn.visibility = View.GONE
    }

    private fun generateOutfitWithAi() {
        if (allClothesList.size < 3) {
            Toast.makeText(this, "Dolabında en az 3 parça olmalı kanka!", Toast.LENGTH_SHORT).show()
            return
        }
        if (isFinishing) return

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_kombin, null)
        dialog.setContentView(view)

        val tvAi = view.findViewById<TextView>(R.id.tvGeminiComment)
        val ivTop = view.findViewById<ImageView>(R.id.ivSuggestTop)
        val ivBottom = view.findViewById<ImageView>(R.id.ivSuggestBottom)
        val ivShoes = view.findViewById<ImageView>(R.id.ivSuggestShoes)

        tvAi.text = "Gardırobun taranıyor... ✨"
        dialog.show()
        setBottomSheetWidth(dialog)

        val wardrobeInventory = allClothesList.joinToString(separator = "\n") {
            "ID: ${it.id} | Kategori ${it.category_id} | Renk: ${it.color} | Detay: ${it.ai_description}"
        }

        val systemPrompt = """
            Sen profesyonel bir stil danışmanısın. Gardırop: $wardrobeInventory
            Lütfen birbirine en uyumlu 1 Üst (Kat 1), 1 Alt (Kat 2) ve 1 Ayakkabı (Kat 3) seç.
            Cevabını SADECE şu JSON formatında ver:
            {"yorum": "Mesajın", "ust_id": ID, "alt_id": ID, "ayakkabi_id": ID}
        """.trimIndent()

        // GÜVENLİ ANAHTAR KULLANIMI
        val generativeModel = GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        lifecycleScope.launch {
            try {
                val responseText = generativeModel.generateContent(systemPrompt).text ?: ""
                val cleanJsonString = responseText.replace("```json", "").replace("```", "").trim()
                val jsonObject = JSONObject(cleanJsonString)

                if (!isFinishing && dialog.isShowing) {
                    tvAi.text = jsonObject.getString("yorum")
                    val ustId = jsonObject.getInt("ust_id")
                    val altId = jsonObject.getInt("alt_id")
                    val ayakkabiId = jsonObject.getInt("ayakkabi_id")

                    allClothesList.find { it.id == ustId }?.let { Glide.with(this@MainActivity).load(it.image_url).into(ivTop) }
                    allClothesList.find { it.id == altId }?.let { Glide.with(this@MainActivity).load(it.image_url).into(ivBottom) }
                    allClothesList.find { it.id == ayakkabiId }?.let { Glide.with(this@MainActivity).load(it.image_url).into(ivShoes) }
                }
            } catch (e: Exception) {
                if (!isFinishing) tvAi.text = "AI meşgul: ${e.localizedMessage}"
                Log.e("GeminiError", "Detay: ", e)
            }
        }
    }

    private fun openKombinReviewPanel() {
        if (isFinishing) return
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_review, null)
        dialog.setContentView(view)

        val tvAi = view.findViewById<TextView>(R.id.tvAiResponse)
        val pb = view.findViewById<ProgressBar>(R.id.pbLoading)

        selectedTop?.let { Glide.with(this).load(it.image_url).into(view.findViewById(R.id.ivReviewTop)) }
        selectedBottom?.let { Glide.with(this).load(it.image_url).into(view.findViewById(R.id.ivReviewBottom)) }
        selectedShoes?.let { Glide.with(this).load(it.image_url).into(view.findViewById(R.id.ivReviewShoes)) }

        dialog.show()
        setBottomSheetWidth(dialog)

        val currentSelection = "Üst: ${selectedTop?.color ?: "Yok"}, Alt: ${selectedBottom?.color ?: "Yok"}, Ayakkabı: ${selectedShoes?.color ?: "Yok"}"
        callGeminiAPI("Şu kombini yorumla: $currentSelection", tvAi, pb)
    }

    private fun callGeminiAPI(prompt: String, textView: TextView, progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
        // GÜVENLİ ANAHTAR KULLANIMI
        val generativeModel = GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
        lifecycleScope.launch {
            try {
                val result = generativeModel.generateContent(prompt).text
                if (!isFinishing) textView.text = result
            } catch (e: Exception) {
                Log.e("GeminiError", "Detay:", e)
                if (!isFinishing) textView.text = "Hata detayı: ${e.localizedMessage}"
            } finally {
                if (!isFinishing) progressBar.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_ai_suggest) { suggestKombinWithAI(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun suggestKombinWithAI() {
        if (allClothesList.isEmpty() || isFinishing) return
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_review, null)
        dialog.setContentView(view)
        dialog.show()
        setBottomSheetWidth(dialog)
        callGeminiAPI("Gardırobumdan kombin öner.", view.findViewById(R.id.tvAiResponse), view.findViewById(R.id.pbLoading))
    }
}