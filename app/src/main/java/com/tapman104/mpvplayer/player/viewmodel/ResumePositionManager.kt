package com.tapman104.mpvplayer.player.viewmodel

import com.tapman104.mpvplayer.core.database.ResumePositionDao
import com.tapman104.mpvplayer.core.database.ResumePositionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ResumePositionManager(
    private val resumePositionDao: ResumePositionDao,
    private var coroutineScope: CoroutineScope? = null,
    private var getPlayerDurationMs: (() -> Long)? = { 0L }
) {
    fun attach(scope: CoroutineScope, getDuration: () -> Long) {
        this.coroutineScope = scope
        this.getPlayerDurationMs = getDuration
    }

    /**
     * Saves the current playback position for [filePath].
     * Positions under 5 seconds are ignored to avoid saving very early scrubs.
     */
    fun saveCurrentPosition(filePath: String, positionMs: Long) {
        coroutineScope?.launch {
            val duration = getPlayerDurationMs?.invoke() ?: 0L
            if (positionMs > 5000L && (duration == 0L || positionMs < duration - 5000L)) {
                resumePositionDao.savePosition(
                    ResumePositionEntity(filePath = filePath, positionMs = positionMs)
                )
            } else if (duration > 0L && positionMs >= duration - 5000L) {
                resumePositionDao.deletePosition(filePath)
            }
        }
    }

    /**
     * Loads the saved resume position for [filePath] and delivers it via [onResult].
     * Returns null if no position is stored.
     */
    fun loadResumePosition(filePath: String, onResult: (Long?) -> Unit) {
        coroutineScope?.launch {
            val entity = resumePositionDao.getPosition(filePath)
            onResult(entity?.positionMs)
        }
    }

    /** Deletes the saved resume position for [filePath] (e.g. user chose "Start Over"). */
    fun clearResumePosition(filePath: String) {
        coroutineScope?.launch {
            resumePositionDao.deletePosition(filePath)
        }
    }

    fun save(path: String, posMs: Long) = saveCurrentPosition(path, posMs)

    suspend fun get(path: String): Long? = resumePositionDao.getPosition(path)?.positionMs
}
