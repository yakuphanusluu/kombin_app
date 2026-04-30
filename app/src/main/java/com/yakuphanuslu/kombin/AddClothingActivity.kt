package com.yakuphanuslu.kombin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

// BuildConfig otomatik gelmezse bu satırın aktif olduğundan emin ol kanka
// import com.yakuphanuslu.kombin.BuildConfig

class AddClothingActivity : AppCompatActivity() {

    private lateinit var ivClothing: ImageView
    private lateinit var tvAiResult: TextView
    private lateinit var btnSave: Button

    private var capturedBitmap: Bitmap? = null
    private var aiDescription: String = ""
    private var aiColor: String = ""
    private var categoryId: Int = 1

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Kamera izni verilmediği için fotoğraf çekilemez.", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Deprecated uyarısı almamak için güvenli dönüşüm kullanıyoruz
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                capturedBitmap = it
                ivClothing.setImageBitmap(it)
                analyzeImageWithGemini(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_clothing)

        ivClothing = findViewById(R.id.ivClothing)
        tvAiResult = findViewById(R.id.tvAiResult)
        btnSave = findViewById(R.id.btnSave)
        val btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        btnTakePhoto.setOnClickListener {
            checkPermissionAndOpenCamera()
        }

        btnSave.setOnClickListener {
            uploadToPHP()
        }
    }

    private fun checkPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePicturePreview.launch(takePictureIntent)
    }

    private fun analyzeImageWithGemini(bitmap: Bitmap) {
        tvAiResult.text = "Gemini analiz ediyor, lütfen bekle... ✨"
        btnSave.isEnabled = false

        // GÜVENLİ ANAHTAR KULLANIMI: MainActivity'de yaptığımız gibi BuildConfig'den çekiyoruz
        val generativeModel = GenerativeModel(
            modelName = "gemini-3-flash-preview",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prompt = "Bu kıyafeti analiz et. Bana sadece şu iki bilgiyi virgülle ayırarak ver: [Renk], [Kıyafet Türü]. Örneğin: Kırmızı, Tişört. Başka hiçbir açıklama yazma."

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val resultText = response.text ?: "Analiz edilemedi"

                withContext(Dispatchers.Main) {
                    if (!isFinishing) {
                        tvAiResult.text = "Analiz: $resultText"

                        val parts = resultText.split(",")
                        if(parts.size >= 2) {
                            aiColor = parts[0].trim()
                            aiDescription = parts[1].trim()
                        } else {
                            aiDescription = resultText
                        }

                        btnSave.isEnabled = true
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isFinishing) {
                        tvAiResult.text = "Hata: ${e.localizedMessage}"
                        Log.e("GeminiError", "Kıyafet analizi patladı: ", e)
                    }
                    btnSave.isEnabled = true
                }
            }
        }
    }

    private fun uploadToPHP() {
        val sharedPref = getSharedPreferences("KombinApp", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Kullanıcı bilgisi bulunamadı, tekrar giriş yapın.", Toast.LENGTH_SHORT).show()
            return
        }

        if (capturedBitmap == null) {
            Toast.makeText(this, "Lütfen önce bir fotoğraf çekin.", Toast.LENGTH_SHORT).show()
            return
        }

        tvAiResult.text = "Sunucuya yükleniyor..."
        btnSave.isEnabled = false

        val retrofit = Retrofit.Builder()
            .baseUrl("https://yakuphanuslu.com/kombin_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val uId = userId.toString().toRequestBody(MultipartBody.FORM)
        val cId = categoryId.toString().toRequestBody(MultipartBody.FORM)
        val color = aiColor.toRequestBody(MultipartBody.FORM)
        val desc = aiDescription.toRequestBody(MultipartBody.FORM)

        val stream = ByteArrayOutputStream()
        capturedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()

        val mediaType = "image/jpg".toMediaTypeOrNull()
        val requestFile = byteArray.toRequestBody(mediaType)
        val imagePart = MultipartBody.Part.createFormData("image", "photo_${System.currentTimeMillis()}.jpg", requestFile)

        apiService.uploadClothing(uId, cId, color, desc, imagePart).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (!isFinishing) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(this@AddClothingActivity, "Kıyafet başarıyla kaydedildi!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        tvAiResult.text = "Sunucu hatası: ${response.body()?.message}"
                        btnSave.isEnabled = true
                    }
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                if (!isFinishing) {
                    tvAiResult.text = "Bağlantı hatası: ${t.message}"
                    btnSave.isEnabled = true
                }
            }
        })
    }
}