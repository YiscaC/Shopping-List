package com.example.shoppinglist.data.local.models

import android.graphics.Bitmap

data class Message(
    val senderId: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    var previewImageBitmap: Bitmap? = null

)