package com.example.notepad

import androidx.room.TypeConverter
import com.example.notepad.CreatNotes1.ChecklistItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    @TypeConverter
    fun fromChecklistList(value: List<ChecklistItem>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toChecklistList(value: String?): List<ChecklistItem>? {
        return if (value.isNullOrEmpty()) null
        else Gson().fromJson(value, object : TypeToken<List<ChecklistItem>>() {}.type)
    }

}
