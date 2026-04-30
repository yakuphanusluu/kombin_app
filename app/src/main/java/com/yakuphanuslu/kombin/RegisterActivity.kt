package com.yakuphanuslu.kombin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://yakuphanuslu.com/kombin_api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        findViewById<Button>(R.id.btnRegisterAction).setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
            val user = findViewById<EditText>(R.id.etNewUsername).text.toString().trim()
            val pass = findViewById<EditText>(R.id.etNewPassword).text.toString().trim()

            if (email.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Tüm alanları doldur kanka!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            apiService.register(email, user, pass).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    val registerData = response.body()
                    if (response.isSuccessful && registerData?.status == "success") {

                        // Şık ve Net Bir Onay Bilgilendirmesi
                        AlertDialog.Builder(this@RegisterActivity)
                            .setTitle("Kayıt Başarılı! 🎉")
                            .setMessage("Hesabın oluşturuldu kanka. Giriş yapabilmek için lütfen e-posta kutuna (ve spam klasörüne) gelen onay linkini kontrol et.")
                            .setCancelable(false)
                            .setPositiveButton("Anladım, Girişe Git") { _, _ ->
                                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .show()

                    } else {
                        Toast.makeText(this@RegisterActivity, "Hata: ${registerData?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity, "Bağlantı Hatası: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}