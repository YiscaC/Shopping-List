package com.example.shoppinglist.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ParticipantsConverter {

    @TypeConverter
    fun fromParticipantsMap(participants: Map<String, Boolean>): String {
        return Gson().toJson(participants)
    }

    @TypeConverter
    fun toParticipantsMap(participantsString: String): Map<String, Boolean> {
        val type = object : TypeToken<Map<String, Boolean>>() {}.type
        return Gson().fromJson(participantsString, type) ?: emptyMap()
    }
}
