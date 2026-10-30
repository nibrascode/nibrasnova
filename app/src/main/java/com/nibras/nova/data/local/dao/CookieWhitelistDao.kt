package com.nibras.nova.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nibras.nova.data.local.entity.CookieWhitelistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CookieWhitelistDao {
    @Query("SELECT * FROM cookie_whitelist ORDER BY domain ASC")
    fun observeWhitelistedDomains(): Flow<List<CookieWhitelistEntity>>

    @Query("SELECT * FROM cookie_whitelist")
    suspend fun getAllWhitelistedDomainsOnce(): List<CookieWhitelistEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addDomain(entity: CookieWhitelistEntity): Long

    @Query("DELETE FROM cookie_whitelist WHERE id = :id")
    suspend fun removeDomainById(id: Long)
}
