package com.guoyuan.memoria.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface AppDao {
    // TextEntity operations
    @Insert
    suspend fun insertText(text: TextEntity)

    @Query("SELECT * FROM texts")
    suspend fun getAllTexts(): List<TextEntity>

    // PunctuationEntity operations
    @Insert
    suspend fun insertPunctuation(punctuation: PunctuationEntity)

    @Update
    suspend fun updatePunctuation(punctuation: PunctuationEntity)

    @Delete
    suspend fun deletePunctuation(punctuation: PunctuationEntity)

    @Query("SELECT * FROM punctuations")
    suspend fun getAllPunctuations(): List<PunctuationEntity>

    @Query("SELECT * FROM punctuations WHERE is_enabled = 1")
    suspend fun getEnabledPunctuations(): List<PunctuationEntity>
}
