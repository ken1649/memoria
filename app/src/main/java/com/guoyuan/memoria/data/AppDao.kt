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

    @Query("UPDATE texts SET is_favorite = 0")
    suspend fun clearAllFavorites()

    @Query("UPDATE texts SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Int, isFavorite: Boolean)

    @Query("UPDATE texts SET display_order = :order WHERE id = :id")
    suspend fun updateDisplayOrder(id: Int, order: Int)

    @Update
    suspend fun updateText(text: TextEntity)

    @Delete
    suspend fun deleteText(text: TextEntity)

    @Query("DELETE FROM texts WHERE id = :id")
    suspend fun deleteText(id: Int)

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
