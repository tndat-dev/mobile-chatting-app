package com.example.myapplication.data.database

import androidx.room.TypeConverter
import com.example.myapplication.data.model.RequestStatus

class Converters {
    @TypeConverter
    fun fromRequestStatus(value: RequestStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toRequestStatus(value: String): RequestStatus {
        return try {
            RequestStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            RequestStatus.PENDING
        }
    }
}
