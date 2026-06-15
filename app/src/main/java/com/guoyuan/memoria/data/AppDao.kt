package com.guoyuan.memoria.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AppDao {
    @Insert
    suspend fun insertText(text: TextEntity)

    @Query("SELECT * FROM texts")
    suspend fun getAllTexts(): List<TextEntity>

    @Update
    suspend fun updatePunctuation(punctuation: PunctuationEntity)

    @Query("SELECT * FROM punctuations")
    suspend fun getAllPunctuations(): List<PunctuationEntity>
}
