package com.comet.data.repo

import com.comet.data.db.CometDatabase
import com.comet.data.db.EngineStateEntity
import com.comet.data.model.EngineChannel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Engine version/channel bookkeeping (Part 7.4, S9). */
@Singleton
class EngineStateRepository @Inject constructor(
    private val db: CometDatabase,
) {
    private val dao = db.engineStateDao()

    fun observe(): Flow<EngineStateEntity?> = dao.observe()

    suspend fun get(): EngineStateEntity? = dao.get()

    suspend fun recordCheck(channel: EngineChannel, version: String) {
        val current = dao.get()
        dao.upsert(
            EngineStateEntity(
                version = version,
                channel = channel,
                lastCheckAt = System.currentTimeMillis(),
                lastUpdateAt = current?.lastUpdateAt ?: 0L,
            )
        )
    }

    suspend fun recordUpdate(channel: EngineChannel, version: String) {
        dao.upsert(
            EngineStateEntity(
                version = version,
                channel = channel,
                lastCheckAt = System.currentTimeMillis(),
                lastUpdateAt = System.currentTimeMillis(),
            )
        )
    }
}
