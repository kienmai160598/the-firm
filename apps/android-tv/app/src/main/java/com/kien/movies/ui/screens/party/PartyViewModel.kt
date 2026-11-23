package com.kien.movies.ui.screens.party

import androidx.lifecycle.ViewModel
import com.kien.movies.core.party.PartyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PartyViewModel @Inject constructor(val party: PartyRepository) : ViewModel()
