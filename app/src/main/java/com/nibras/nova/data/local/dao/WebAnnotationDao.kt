package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nibras.nova.data.local.entity.WebAnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebAnnotationDao {
    @Query("SELECT * FROM web_annotations ORDER BY createdAt DESC")
    fun observeAllAnnotations(): Flow<List<WebAnnotationEntity>>

    @Query("SELECT * FROM web_annotations WHERE pageUrl = :url ORDER BY createdAt DESC")
    fun observeAnnotationsForUrl(url: String): Flow<List<WebAnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(entity: WebAnnotationEntity): Long

    @Query("DELETE FROM web_annotations WHERE id = :id")
    suspend fun deleteAnnotation(id: Long)
}
