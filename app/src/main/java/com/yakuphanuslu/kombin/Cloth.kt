package com.yakuphanuslu.kombin

data class Cloth(
    val id: Int,
    val user_id: Int,
    val category_id: Int, // Bunu ekledik!
    val image_url: String,
    val color: String,
    val ai_description: String
)