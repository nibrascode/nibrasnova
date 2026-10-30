package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nibras.nova.data.local.entity.UserScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserScriptDao {
    @Query("SELECT * FROM user_scripts ORDER BY createdAt DESC")
    fun observeAllScripts(): Flow<List<UserScriptEntity>>

    @Query("SELECT * FROM user_scripts WHERE isEnabled = 1 ORDER BY id ASC")
    suspend fun getEnabledScriptsOnce(): List<UserScriptEntity>

    @Query("SELECT COUNT(*) FROM user_scripts")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: UserScriptEntity): Long

    @Update
    suspend fun updateScript(script: UserScriptEntity)

    @Query("DELETE FROM user_scripts WHERE id = :id")
    suspend fun deleteScript(id: Long)
}
