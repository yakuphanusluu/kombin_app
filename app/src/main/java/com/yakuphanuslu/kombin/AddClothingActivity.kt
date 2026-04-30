package com.yakuphanuslu.kombin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
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

class AddClothingActivity : AppCompatActivity() {

    private lateinit var ivClothing: ImageView
    private lateinit var tvAiResult: TextView
    private lateinit var btnSave: Button

    private var capturedBitmap: Bitmap? = null
    private var aiDescription: String = ""
    private var aiColor: String = ""

    // BAŞLANGIÇTA 1 (Üst Giyim) AMA ANALİZ SONRASI DEĞİŞECEK
    private var categoryId: Int = 1

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) openCamera()
        else Toast.makeText(this, "Kamera izni verilmedi.", Toast.LENGTH_SHORT).show()
    }

    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                capturedBitmap = it
                ivClothing.setImageBitmap(it)
                analyzeImageWithGemini(it)
            }
        }
    }

    private val pickImageGallery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                capturedBitmap = bitmap
                ivClothing.setImageBitmap(bitmap)
                analyzeImageWithGemini(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val btnOpenGallery = findViewById<Button>(R.id.btnOpenGallery)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }
        btnTakePhoto.setOnClickListener { checkPermissionAndOpenCamera() }
        btnOpenGallery.setOnClickListener {
            pickImageGallery.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        btnSave.setOnClickListener { uploadToPHP() }
    }

    private fun checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePicturePreview.launch(takePictureIntent)
    }

    private fun analyzeImageWithGemini(bitmap: Bitmap) {
        tvAiResult.text = "Gemini analiz ediyor... ✨"
        btnSave.isEnabled = false

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prompt = "Bu kıyafeti analiz et. Bana sadece şu iki bilgiyi virgülle ayırarak ver: [Renk], [Kıyafet Türü]. Örneğin: Kırmızı, Tişört veya Mavi, Pantolon. Başka hiçbir açıklama yazma."

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

                        // --- BURASI KRİTİK: Kategori ID'sini burada güncelliyoruz ---
                        val descLower = aiDescription.lowercase()
                        categoryId = when {
                            descLower.contains("ayakkabı") || descLower.contains("bot") || descLower.contains("sneaker") -> 4
                            descLower.contains("pantolon") || descLower.contains("şort") || descLower.contains("etek") -> 2
                            else -> 1 // Varsayılan olarak Üst Giyim
                        }
                        // ---------------------------------------------------------

                        btnSave.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isFinishing) tvAiResult.text = "Hata: ${e.localizedMessage}"
                    btnSave.isEnabled = true
                }
            }
        }
    }

    private fun uploadToPHP() {
        val sharedPref = getSharedPreferences("KombinApp", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        if (userId == -1 || capturedBitmap == null) return

        tvAiResult.text = "Sunucuya kaydediliyor..."
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
        val imagePart = MultipartBody.Part.createFormData("image", "photo.jpg", stream.toByteArray().toRequestBody("image/jpg".toMediaTypeOrNull()))

        apiService.uploadClothing(uId, cId, color, desc, imagePart).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@AddClothingActivity, "Başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    tvAiResult.text = "Hata: ${response.body()?.message}"
                    btnSave.isEnabled = true
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                tvAiResult.text = "Bağlantı hatası: ${t.message}"
                btnSave.isEnabled = true
            }
        })
    }
}