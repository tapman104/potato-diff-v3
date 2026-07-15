package com.tapman104.mpvplayer.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumePositionDao {
    @Query("SELECT * FROM resume_positions WHERE filePath = :filePath LIMIT 1")
    suspend fun getPosition(filePath: String): ResumePositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePosition(entity: ResumePositionEntity)

    @Query("DELETE FROM resume_positions WHERE filePath = :filePath")
    suspend fun deletePosition(filePath: String)

    @Query("SELECT * FROM resume_positions ORDER BY updatedAt DESC LIMIT 50")
    fun getAllPositions(): Flow<List<ResumePositionEntity>>
}
