package com.emman.pomodoro.data.repository

import com.emman.pomodoro.data.local.dao.TagDao
import com.emman.pomodoro.data.local.entity.TagEntity
import com.emman.pomodoro.data.mapper.toDomain
import com.emman.pomodoro.data.mapper.toEntity
import com.emman.pomodoro.domain.model.Tag
import com.emman.pomodoro.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {

    override val tags: Flow<List<Tag>> =
        tagDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun createTag(name: String, colorArgb: Long): Long =
        tagDao.insert(TagEntity(name = name, colorArgb = colorArgb))

    override suspend fun updateTag(tag: Tag) {
        tagDao.update(tag.toEntity())
    }

    override suspend fun deleteTag(id: Long) {
        tagDao.delete(id)
    }

    override suspend fun importTags(tags: List<Tag>) {
        tagDao.insertAll(tags.map { it.toEntity() })
    }
}
