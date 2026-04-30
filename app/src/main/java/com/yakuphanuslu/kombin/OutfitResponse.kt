package com.yakuphanuslu.kombin

data class OutfitResponse(
    val status: String,
    val message: String?, // Hata durumunda mesaj gelirse diye
    val yorum: String?,
    val ust_id: Int?,
    val alt_id: Int?,
    val ayakkabi_id: Int?
)