package com.yakuphanuslu.kombin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OTURUM KONTROLÜ
        val sharedPref = getSharedPreferences("KombinApp", Context.MODE_PRIVATE)
        if (sharedPref.getInt("user_id", -1) != -1) {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://yakuphanuslu.com/kombin_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // GİRİŞ YAP BUTONU
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val user = findViewById<EditText>(R.id.etUsername).text.toString()
            val pass = findViewById<EditText>(R.id.etPassword).text.toString()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Alanları doldur kanka!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            apiService.login(user, pass).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    try {
                        val loginData = response.body()
                        if (response.isSuccessful && loginData?.status == "success") {

                            val userId = loginData.user_id ?: -1
                            val userName = loginData.username ?: "Kullanıcı"

                            sharedPref.edit().apply {
                                putInt("user_id", userId)
                                putString("username", userName)
                                apply()
                            }

                            Toast.makeText(this@LoginActivity, "Hoş geldin $userName!", Toast.LENGTH_SHORT).show()

                            // GEÇİŞİ DÜZELTEN KISIM:
                            // Intent bayrakları ile LoginActivity'yi stack'ten tamamen temizliyoruz.
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            finish()
                        } else {
                            val errorMsg = loginData?.message ?: "Giriş başarısız."
                            Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "Yazılım Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Bağlantı Hatası: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            showForgotPasswordDialog()
        }

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // --- ŞİFREMİ UNUTTUM DİALOGLARI ---

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Şifremi Unuttum")
        val input = EditText(this)
        input.hint = "E-posta Adresin"

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(50, 30, 50, 10)
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Kod Gönder") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                apiService.sendResetCode("send_reset_code", email).enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            Toast.makeText(this@LoginActivity, "Kod gönderildi kanka!", Toast.LENGTH_SHORT).show()
                            showVerifyAndResetDialog(email)
                        } else {
                            Toast.makeText(this@LoginActivity, response.body()?.message ?: "Hata", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                        Toast.makeText(this@LoginActivity, "Bağlantı Hatası!", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
        builder.setNegativeButton("İptal", null)
        builder.show()
    }

    private fun showVerifyAndResetDialog(email: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Yeni Şifre Belirle")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 30, 50, 10)

        val etCode = EditText(this)
        etCode.hint = "6 Haneli Kod"
        layout.addView(etCode)

        val etPass = EditText(this)
        etPass.hint = "Yeni Şifre"
        etPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(etPass)

        builder.setView(layout)

        builder.setPositiveButton("Güncelle") { _, _ ->
            val code = etCode.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (code.isNotEmpty() && pass.isNotEmpty()) {
                apiService.resetPassword("reset_password", email, code, pass).enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            Toast.makeText(this@LoginActivity, "Şifre güncellendi!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@LoginActivity, response.body()?.message ?: "Hata", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                        Toast.makeText(this@LoginActivity, "Bağlantı Hatası!", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
        builder.setNegativeButton("İptal", null)
        builder.show()
    }
}