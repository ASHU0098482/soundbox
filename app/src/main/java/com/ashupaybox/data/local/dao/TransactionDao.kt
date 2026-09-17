package com.ashupaybox.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ashupaybox.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class MethodRevenueTuple(
    @ColumnInfo(name = "paymentMethod") val method: String,
    @ColumnInfo(name = "totalPaise") val totalPaise: Long,
    @ColumnInfo(name = "txCount") val count: Int
)

data class HourlyRevenueTuple(
    @ColumnInfo(name = "hourBucket") val hourBucket: Int,
    @ColumnInfo(name = "totalPaise") val totalPaise: Long,
    @ColumnInfo(name = "txCount") val count: Int
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE eventId = :eventId LIMIT 1")
    suspend fun getTransactionByEventId(eventId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE paymentId = :paymentId LIMIT 1")
    suspend fun getTransactionByPaymentId(paymentId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE (:includeTest = 1 OR isTest = 0) ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(includeTest: Boolean): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE (:includeTest = 1 OR isTest = 0) ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactionsFlow(limit: Int, includeTest: Boolean): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions 
        WHERE status = 'CAPTURED' 
        AND timestamp >= :startMillis AND timestamp < :endMillis
        AND (:includeTest = 1 OR isTest = 0)
    """)
    fun getRevenueBetweenFlow(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions 
        WHERE status = 'CAPTURED' 
        AND timestamp >= :startMillis AND timestamp < :endMillis
        AND (:includeTest = 1 OR isTest = 0)
    """)
    suspend fun getRevenueBetween(startMillis: Long, endMillis: Long, includeTest: Boolean): Long

    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE status = 'CAPTURED' 
        AND timestamp >= :startMillis AND timestamp < :endMillis
        AND (:includeTest = 1 OR isTest = 0)
    """)
    fun getPaymentCountBetweenFlow(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<Int>

    @Query("""
        SELECT COALESCE(AVG(amountPaise), 0) FROM transactions 
        WHERE status = 'CAPTURED' 
        AND timestamp >= :startMillis AND timestamp < :endMillis
        AND (:includeTest = 1 OR isTest = 0)
    """)
    fun getAveragePaymentBetweenFlow(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<Long>

    @Query("""
        SELECT COALESCE(MAX(amountPaise), 0) FROM transactions 
        WHERE status = 'CAPTURED' 
        AND timestamp >= :startMillis AND timestamp < :endMillis
        AND (:includeTest = 1 OR isTest = 0)
    """)
    fun getHighestPaymentBetweenFlow(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<Long>

    @Query("""
        SELECT paymentMethod, COALESCE(SUM(amountPaise), 0) AS totalPaise, COUNT(*) AS txCount
        FROM transactions
        WHERE status = 'CAPTURED'
        AND timestamp >= :startMillis AND timestamp < :endMillis
        AND (:includeTest = 1 OR isTest = 0)
        GROUP BY paymentMethod
    """)
    fun getRevenueByMethodFlow(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<List<MethodRevenueTuple>>

    @Query("""
        SELECT ((timestamp - :startMillis) / 3600000) AS hourBucket,
               COALESCE(SUM(amountPaise), 0) AS totalPaise,
               COUNT(*) AS txCount
        FROM transactions
        WHERE status = 'CAPTURED'
        AND timestamp >= :startMillis AND timestamp < :endMillis
        AND (:includeTest = 1 OR isTest = 0)
        GROUP BY hourBucket
        ORDER BY hourBucket ASC
    """)
    fun getHourlyRevenueFlow(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<List<HourlyRevenueTuple>>

    @Query("""
        SELECT * FROM transactions
        WHERE (:includeTest = 1 OR isTest = 0)
        AND (:method IS NULL OR paymentMethod = :method)
        AND (:status IS NULL OR status = :status)
        AND (:startMillis IS NULL OR timestamp >= :startMillis)
        AND (:endMillis IS NULL OR timestamp < :endMillis)
        AND (
            :searchQuery IS NULL OR :searchQuery = '' OR
            paymentId LIKE '%' || :searchQuery || '%' OR
            orderId LIKE '%' || :searchQuery || '%' OR
            eventId LIKE '%' || :searchQuery || '%' OR
            payerName LIKE '%' || :searchQuery || '%'
        )
        ORDER BY timestamp DESC
    """)
    fun filterTransactionsFlow(
        searchQuery: String?,
        method: String?,
        status: String?,
        startMillis: Long?,
        endMillis: Long?,
        includeTest: Boolean
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE (:includeTest = 1 OR isTest = 0) ORDER BY timestamp DESC")
    suspend fun getAllTransactions(includeTest: Boolean): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE isTest = 1")
    suspend fun deleteTestData(): Int
}
