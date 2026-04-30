package com.yakuphanuslu.kombin

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("user_id") val user_id: Int?, // PHP'den sayı geleceği için Int kalması en iyisi
    @SerializedName("username") val username: String?
)