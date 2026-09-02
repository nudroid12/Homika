package com.homiq.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.homiq.app.data.local.entity.PropertyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query(
        """
        SELECT * FROM properties
        WHERE isDeleted = 0
        ORDER BY isActive DESC, name COLLATE NOCASE ASC
        """,
    )
    fun observeAll(): Flow<List<PropertyEntity>>

    @Query(
        """
        SELECT * FROM properties
        WHERE isDeleted = 0 AND isActive = 1
        ORDER BY name COLLATE NOCASE ASC
        """,
    )
    fun observeActive(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PropertyEntity?

    @Upsert
    suspend fun upsert(entity: PropertyEntity)

    @Query(
        """
        UPDATE properties
        SET isDeleted = 1,
            isActive = 0,
            updatedAtEpochMillis = :updatedAt,
            revision = revision + 1
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: String, updatedAt: Long)
}
