package com.yakuphanuslu.kombin

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @FormUrlEncoded
    @POST("login.php")
    fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("register.php")
    fun register(
        @Field("email") email: String,
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<RegisterResponse>

    // --- Hesap Güncelleme İşlemleri (update_account.php) ---

    @FormUrlEncoded
    @POST("update_account.php")
    fun sendSecurityCode(
        @Field("action") action: String,
        @Field("user_id") userId: Int
    ): Call<RegisterResponse>

    @FormUrlEncoded
    @POST("update_account.php")
    fun updateAccountInfo(
        @Field("action") action: String,
        @Field("user_id") userId: Int,
        @Field("type") type: String,
        @Field("new_value") newValue: String,
        @Field("code") code: String
    ): Call<RegisterResponse>

    @FormUrlEncoded
    @POST("update_account.php")
    fun updatePassword(
        @Field("action") action: String,
        @Field("user_id") userId: Int,
        @Field("new_password") newPassword: String,
        @Field("code") code: String
    ): Call<RegisterResponse>

    // --- Şifremi Unuttum İşlemleri (reset_password.php) ---

    @FormUrlEncoded
    @POST("reset_password.php")
    fun sendResetCode(
        @Field("action") action: String,
        @Field("email") email: String
    ): Call<RegisterResponse>

    @FormUrlEncoded
    @POST("reset_password.php")
    fun resetPassword(
        @Field("action") action: String,
        @Field("email") email: String,
        @Field("code") code: String,
        @Field("new_password") new_password: String // PHP tarafındaki $_POST['new_password'] ile tam uyum
    ): Call<RegisterResponse>

    // --- AI Kombin Oluşturma (generate_outfit.php) ---
    @FormUrlEncoded
    @POST("generate_outfit.php")
    fun generateAiOutfit(
        @Field("user_id") userId: Int
    ): Call<OutfitResponse>

    // -------------------------------------------------------------

    @Multipart
    @POST("upload_cloth.php")
    fun uploadClothing(
        @Part("user_id") userId: RequestBody,
        @Part("category_id") categoryId: RequestBody,
        @Part("color") color: RequestBody,
        @Part("ai_description") description: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<LoginResponse>

    @GET("get_clothes.php")
    fun getClothes(@Query("user_id") userId: Int): Call<List<Cloth>>
}