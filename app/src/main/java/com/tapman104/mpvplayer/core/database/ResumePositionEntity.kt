package com.tapman104.mpvplayer.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resume_positions")
data class ResumePositionEntity(
    @PrimaryKey val filePath: String,
    val positionMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
