package com.emman.pomodoro.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emman.pomodoro.domain.model.Tag
import com.emman.pomodoro.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    val tags: StateFlow<List<Tag>> = tagRepository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createTag(name: String, colorArgb: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { tagRepository.createTag(trimmed, colorArgb) }
    }

    fun renameTag(tag: Tag, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { tagRepository.updateTag(tag.copy(name = trimmed)) }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch { tagRepository.deleteTag(id) }
    }
}
