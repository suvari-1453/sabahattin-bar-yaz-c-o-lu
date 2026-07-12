package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BubbleStateManager {
    private val _currentSpeech = MutableStateFlow<String?>(null)
    val currentSpeech: StateFlow<String?> = _currentSpeech.asStateFlow()

    private val _isBubbleActive = MutableStateFlow(false)
    val isBubbleActive: StateFlow<Boolean> = _isBubbleActive.asStateFlow()

    fun updateSpeech(text: String?) {
        _currentSpeech.value = text
    }

    fun setBubbleActive(active: Boolean) {
        _isBubbleActive.value = active
    }
}
