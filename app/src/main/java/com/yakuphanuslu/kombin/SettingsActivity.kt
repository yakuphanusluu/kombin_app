package com.yakuphanuslu.kombin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvUsernameDisplay: TextView
    private lateinit var btnUpdateInfo: Button
    private lateinit var apiService: ApiService
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // KARANLIK MODU ENGELLE: Tablet ayarlarından bağımsız hep Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Retrofit kurulumu
        val retrofit = Retrofit.Builder()
            .baseUrl("https://yakuphanuslu.com/kombin_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // SharedPreferences verilerini al
        val sharedPref = getSharedPreferences("KombinApp", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", -1)
        val username = sharedPref.getString("username", "Kullanıcı")

        tvUsernameDisplay = findViewById(R.id.tvUsernameDisplay)
        btnUpdateInfo = findViewById(R.id.btnUpdateInfo)

        // Kullanıcı adını bas
        tvUsernameDisplay.text = "Kullanıcı Adı: $username"

        // GERİ BUTONU: MainActivity'ye güvenli dönüş
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnUpdateInfo.setOnClickListener { showUpdateOptionsDialog() }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            sharedPref.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showUpdateOptionsDialog() {
        if (isFinishing) return
        val options = arrayOf("Kullanıcı Adı Değiştir", "E-posta Değiştir", "Şifre Değiştir")
        AlertDialog.Builder(this)
            .setTitle("Neyi güncellemek istersin kanka?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showInputAndSendCode("username")
                    1 -> showInputAndSendCode("email")
                    2 -> showInputAndSendCode("password")
                }
            }.show()
    }

    private fun showInputAndSendCode(type: String) {
        if (isFinishing) return
        val builder = AlertDialog.Builder(this)
        builder.setTitle(when(type) {
            "username" -> "Yeni Kullanıcı Adı"
            "email" -> "Yeni E-posta Adresi"
            else -> "Yeni Şifre"
        })

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Kod Gönder") { _, _ ->
            val newValue = input.text.toString().trim()
            if (newValue.isNotEmpty()) {
                apiService.sendSecurityCode("send_code", userId).enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                        if (!isFinishing && response.isSuccessful && response.body()?.status == "success") {
                            Toast.makeText(this@SettingsActivity, "Güvenlik kodu mailine uçtu! ✨", Toast.LENGTH_SHORT).show()
                            showVerifyCodeDialog(type, newValue)
                        } else if (!isFinishing) {
                            Toast.makeText(this@SettingsActivity, "Hata: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                        if (!isFinishing) Toast.makeText(this@SettingsActivity, "Bağlantı kesildi kanka!", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
        builder.setNegativeButton("İptal", null)
        builder.show()
    }

    private fun showVerifyCodeDialog(type: String, newValue: String) {
        if (isFinishing) return
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Mailindeki Kodu Gir")
        val input = EditText(this)
        input.hint = "6 Haneli Kod"
        builder.setView(input)

        builder.setPositiveButton("Doğrula ve Güncelle") { _, _ ->
            val code = input.text.toString().trim()

            val callback = object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    if (!isFinishing && response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(this@SettingsActivity, "Başarıyla güncellendi!", Toast.LENGTH_SHORT).show()

                        if (type == "username") {
                            val sharedPref = getSharedPreferences("KombinApp", Context.MODE_PRIVATE)
                            sharedPref.edit().putString("username", newValue).apply()
                            tvUsernameDisplay.text = "Kullanıcı Adı: $newValue"
                        }
                    } else if (!isFinishing) {
                        Toast.makeText(this@SettingsActivity, "Hata: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    if (!isFinishing) Toast.makeText(this@SettingsActivity, "Bağlantı hatası!", Toast.LENGTH_SHORT).show()
                }
            }

            if (type == "password") {
                apiService.updatePassword("update_password", userId, newValue, code).enqueue(callback)
            } else {
                apiService.updateAccountInfo("update_info", userId, type, newValue, code).enqueue(callback)
            }
        }
        builder.setNegativeButton("İptal", null)
        builder.show()
    }
}