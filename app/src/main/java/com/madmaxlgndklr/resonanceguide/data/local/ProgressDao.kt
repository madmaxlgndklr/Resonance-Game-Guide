package com.madmaxlgndklr.resonanceguide.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Upsert
    suspend fun upsertAll(progress: List<ProgressEntity>)

    @Query("SELECT COUNT(*) FROM progress WHERE type = :type AND completed = 1")
    fun observeCompletedCount(type: String): Flow<Int>

    @Query("DELETE FROM progress")
    suspend fun clearAll()

    @Query("DELETE FROM progress WHERE type = :type")
    suspend fun clearType(type: String)
}
