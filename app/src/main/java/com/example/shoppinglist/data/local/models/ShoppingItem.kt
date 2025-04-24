package com.example.shoppinglist.data.local.models

import android.graphics.Bitmap
import androidx.room.Ignore
import com.example.shoppinglist.data.local.models.Message

data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 1,
    var purchased: Boolean = false,
    var expanded: Boolean = false, // ✅ האם הפריט פתוח או סגור
    var order: Int = 0,
    var messages: List<Message> = emptyList(), // ✅ הוספת הודעות לפריט
    var previewImageBitmap: Bitmap? = null


)
