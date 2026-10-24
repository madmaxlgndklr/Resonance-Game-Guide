package com.madmaxlgndklr.resonanceguide.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ProgressDao {

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Upsert
    suspend fun upsertAll(progress: List<ProgressEntity>)

    @Query("DELETE FROM progress")
    suspend fun clearAll()
}
