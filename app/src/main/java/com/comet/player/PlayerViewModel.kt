package com.comet.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comet.data.db.DownloadEntity
import com.comet.data.repo.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PlayerViewModel @Inject constructor(
    repository: DownloadRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val entity: StateFlow<DownloadEntity?> = repository
        .byIdFlow(savedStateHandle.get<String>("id").orEmpty())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
