package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nibras.nova.data.local.entity.PrivateVaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivateVaultDao {
    @Query("SELECT * FROM private_vault_items ORDER BY createdAt DESC")
    fun observeAllVaultItems(): Flow<List<PrivateVaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(entity: PrivateVaultEntity): Long

    @Query("DELETE FROM private_vault_items WHERE id = :id")
    suspend fun deleteVaultItem(id: Long)

    @Query("DELETE FROM private_vault_items")
    suspend fun clearAllVaultItems()
}
