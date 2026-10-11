package com.emman.pomodoro.domain.repository

import com.emman.pomodoro.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    val tags: Flow<List<Tag>>
    suspend fun createTag(name: String, colorArgb: Long): Long
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(id: Long)
    suspend fun importTags(tags: List<Tag>)
}
