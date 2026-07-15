package com.tapman104.mpvplayer.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tapman104.mpvplayer.core.database.ResumePositionDao
import com.tapman104.mpvplayer.core.database.ResumePositionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dao: ResumePositionDao
) : ViewModel() {

    val history: StateFlow<List<ResumePositionEntity>> =
        dao.getAllPositions()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
