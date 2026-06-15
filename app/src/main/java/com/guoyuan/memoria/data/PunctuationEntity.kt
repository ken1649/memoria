package com.guoyuan.memoria.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "punctuations")
data class PunctuationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "symbol") val symbol: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean
)
