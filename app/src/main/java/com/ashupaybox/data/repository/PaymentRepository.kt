package com.ashupaybox.data.repository

import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.model.PaymentMethod
import com.ashupaybox.core.model.PaymentSource
import com.ashupaybox.core.model.PaymentStatus
import com.ashupaybox.data.local.PayBoxDatabase
import com.ashupaybox.data.local.dao.HourlyRevenueTuple
import com.ashupaybox.data.local.dao.MethodRevenueTuple
import com.ashupaybox.data.local.entity.ProcessedEventEntity
import com.ashupaybox.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class PaymentRepository(private val database: PayBoxDatabase) {

    private val txDao = database.transactionDao()
    private val eventDao = database.processedEventDao()

    fun getAllTransactions(includeTest: Boolean): Flow<List<PaymentEvent>> {
        return txDao.getAllTransactionsFlow(includeTest).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getRecentTransactions(limit: Int = 10, includeTest: Boolean): Flow<List<PaymentEvent>> {
        return txDao.getRecentTransactionsFlow(limit, includeTest).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getAllTransactionsList(includeTest: Boolean): List<PaymentEvent> {
        return txDao.getAllTransactions(includeTest).map { it.toDomain() }
    }

    fun filterTransactions(
        searchQuery: String?,
        method: PaymentMethod?,
        status: PaymentStatus?,
        startMillis: Long?,
        endMillis: Long?,
        includeTest: Boolean
    ): Flow<List<PaymentEvent>> {
        return txDao.filterTransactionsFlow(
            searchQuery = searchQuery,
            method = method?.name,
            status = status?.name,
            startMillis = startMillis,
            endMillis = endMillis,
            includeTest = includeTest
        ).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun isEventProcessed(eventId: String): Boolean {
        return eventDao.isEventProcessed(eventId)
    }

    suspend fun insertTransaction(event: PaymentEvent): Boolean {
        val rowId = txDao.insertTransaction(TransactionEntity.fromDomain(event))
        return rowId > 0
    }

    suspend fun recordProcessedEvent(event: PaymentEvent, announced: Boolean = false) {
        eventDao.insertProcessedEvent(
            ProcessedEventEntity(
                eventId = event.eventId,
                paymentId = event.paymentId,
                processedAt = System.currentTimeMillis(),
                status = event.status.name,
                amountPaise = event.amountPaise,
                announced = announced
            )
        )
    }

    suspend fun markAnnounced(eventId: String) {
        eventDao.markAnnounced(eventId)
    }

    fun getRevenueBetween(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<Long> {
        return txDao.getRevenueBetweenFlow(startMillis, endMillis, includeTest)
    }

    suspend fun getRevenueBetweenInstant(startMillis: Long, endMillis: Long, includeTest: Boolean): Long {
        return txDao.getRevenueBetween(startMillis, endMillis, includeTest)
    }

    fun getPaymentCountBetween(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<Int> {
        return txDao.getPaymentCountBetweenFlow(startMillis, endMillis, includeTest)
    }

    fun getAveragePaymentBetween(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<Long> {
        return txDao.getAveragePaymentBetweenFlow(startMillis, endMillis, includeTest)
    }

    fun getHighestPaymentBetween(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<Long> {
        return txDao.getHighestPaymentBetweenFlow(startMillis, endMillis, includeTest)
    }

    fun getRevenueByMethod(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<List<MethodRevenueTuple>> {
        return txDao.getRevenueByMethodFlow(startMillis, endMillis, includeTest)
    }

    fun getHourlyRevenue(startMillis: Long, endMillis: Long, includeTest: Boolean): Flow<List<HourlyRevenueTuple>> {
        return txDao.getHourlyRevenueFlow(startMillis, endMillis, includeTest)
    }

    suspend fun clearTestData(): Int {
        eventDao.clearTestEvents()
        return txDao.deleteTestData()
    }

    suspend fun seedDemoTransactions() {
        val now = System.currentTimeMillis()
        val oneHour = 3600000L
        val oneDay = 86400000L

        val demoItems = listOf(
            PaymentEvent(
                eventId = "demo_ev_1",
                paymentId = "pay_demo_001",
                orderId = "order_demo_101",
                amountPaise = 3500, // ₹35
                status = PaymentStatus.CAPTURED,
                paymentMethod = PaymentMethod.UPI,
                payerName = "Rohan Sharma",
                payerVpaMasked = "rohan***@okaxis",
                timestamp = now - (20 * 60 * 1000),
                isTest = true,
                source = PaymentSource.LOCAL_TEST
            ),
            PaymentEvent(
                eventId = "demo_ev_2",
                paymentId = "pay_demo_002",
                orderId = "order_demo_102",
                amountPaise = 30000, // ₹300
                status = PaymentStatus.CAPTURED,
                paymentMethod = PaymentMethod.UPI,
                payerName = "Pooja Verma",
                payerVpaMasked = "pooja***@paytm",
                timestamp = now - (45 * 60 * 1000),
                isTest = true,
                source = PaymentSource.LOCAL_TEST
            ),
            PaymentEvent(
                eventId = "demo_ev_3",
                paymentId = "pay_demo_003",
                orderId = "order_demo_103",
                amountPaise = 150000, // ₹1,500
                status = PaymentStatus.CAPTURED,
                paymentMethod = PaymentMethod.CARD,
                payerName = "Vikram Malhotra",
                payerVpaMasked = null,
                timestamp = now - (2 * oneHour),
                isTest = true,
                source = PaymentSource.LOCAL_TEST
            ),
            PaymentEvent(
                eventId = "demo_ev_4",
                paymentId = "pay_demo_004",
                orderId = "order_demo_104",
                amountPaise = 125000, // ₹1,250
                status = PaymentStatus.CAPTURED,
                paymentMethod = PaymentMethod.UPI,
                payerName = "Ananya Iyer",
                payerVpaMasked = "ananya***@ybl",
                timestamp = now - (4 * oneHour),
                isTest = true,
                source = PaymentSource.LOCAL_TEST
            ),
            // Yesterday payments
            PaymentEvent(
                eventId = "demo_ev_5",
                paymentId = "pay_demo_005",
                orderId = "order_demo_105",
                amountPaise = 250000, // ₹2,500
                status = PaymentStatus.CAPTURED,
                paymentMethod = PaymentMethod.NETBANKING,
                payerName = "Suresh Patel",
                timestamp = now - oneDay - (3 * oneHour),
                isTest = true,
                source = PaymentSource.LOCAL_TEST
            ),
            PaymentEvent(
                eventId = "demo_ev_6",
                paymentId = "pay_demo_006",
                orderId = "order_demo_106",
                amountPaise = 813000, // ₹8,130
                status = PaymentStatus.CAPTURED,
                paymentMethod = PaymentMethod.UPI,
                payerName = "Amitabh Sen",
                timestamp = now - oneDay - (6 * oneHour),
                isTest = true,
                source = PaymentSource.LOCAL_TEST
            )
        )

        for (item in demoItems) {
            if (!eventDao.isEventProcessed(item.eventId)) {
                txDao.insertTransaction(TransactionEntity.fromDomain(item))
                eventDao.insertProcessedEvent(
                    ProcessedEventEntity(
                        eventId = item.eventId,
                        paymentId = item.paymentId,
                        processedAt = item.timestamp,
                        status = item.status.name,
                        amountPaise = item.amountPaise,
                        announced = true
                    )
                )
            }
        }
    }
}
