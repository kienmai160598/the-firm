package com.kien.movies.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kien.movies.data.local.ExperienceModeDataStore
import com.kien.movies.domain.model.ExperienceMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class ExperienceModeSettingsViewModel @Inject constructor(
    private val experienceModeDataStore: ExperienceModeDataStore
) : ViewModel() {
    val mode: Flow<ExperienceMode?> = experienceModeDataStore.mode

    fun setMode(mode: ExperienceMode) {
        viewModelScope.launch {
            experienceModeDataStore.setMode(mode)
        }
    }
}
