package com.example.shoppinglist.data.local.converters

import androidx.room.TypeConverter
import com.example.shoppinglist.data.local.models.Message
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MessagesConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromMessagesList(messages: List<Message>?): String {
        return gson.toJson(messages)
    }

    @TypeConverter
    fun toMessagesList(data: String?): List<Message> {
        if (data.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<Message>>() {}.type
        return gson.fromJson(data, listType)
    }
}
