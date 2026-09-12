package com.saferouteai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.saferouteai.data.local.entity.TrustedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY createdAt ASC")
    fun getAllContacts(): Flow<List<TrustedContactEntity>>

    @Query("SELECT * FROM trusted_contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): TrustedContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(contact: TrustedContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE trusted_contacts SET isEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateEnabled(id: String, isEnabled: Boolean, updatedAt: Long)
}
