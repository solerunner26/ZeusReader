package com.solerunner26.zeusreader.data

import com.solerunner26.zeusreader.data.local.LibraryDao
import com.solerunner26.zeusreader.data.local.WorkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface LibraryRepository {
    fun observeWorks(): Flow<List<WorkEntity>>
    fun searchWorks(ftsQuery: String): Flow<List<WorkEntity>>
}

class RoomLibraryRepository @Inject constructor(
    private val libraryDao: LibraryDao
) : LibraryRepository {
    override fun observeWorks(): Flow<List<WorkEntity>> = libraryDao.observeActiveWorks()

    override fun searchWorks(ftsQuery: String): Flow<List<WorkEntity>> =
        libraryDao.searchActiveWorks(ftsQuery)
}
