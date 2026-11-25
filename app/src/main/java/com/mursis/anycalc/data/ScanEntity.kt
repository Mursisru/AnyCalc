package com.mursis.anycalc.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val short: String,
    val detailed: String,
    val confidence: Float,
    val provider: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans ORDER BY createdAt DESC")
    suspend fun all(): List<ScanEntity>

    @Query(
        "SELECT * FROM scans WHERE lower(text) LIKE '%' || lower(:query) || '%' " +
            "OR lower(short) LIKE '%' || lower(:query) || '%' ORDER BY createdAt DESC",
    )
    fun search(query: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE text = :text ORDER BY createdAt DESC LIMIT 1")
    suspend fun findByText(text: String): ScanEntity?

    @Insert
    suspend fun insert(scan: ScanEntity): Long

    @Delete
    suspend fun delete(scan: ScanEntity)

    @Query("DELETE FROM scans")
    suspend fun clear()
}
