package com.ashupaybox.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ashupaybox.core.model.PaymentEvent
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportTransactionsToCsv(context: Context, transactions: List<PaymentEvent>): Uri? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(exportDir, "paybox_transactions_$timestamp.csv")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

            FileWriter(file).use { writer ->
                // Header
                writer.append("Date,Time,Amount (INR),Amount (Paise),Status,Method,Payment ID,Order ID,Event ID,Payer,Source,IsTest\n")

                for (tx in transactions) {
                    val date = Date(tx.timestamp)
                    val dateStr = dateFormat.format(date)
                    val timeStr = timeFormat.format(date)
                    val amountInr = (tx.amountPaise / 100.0).toString()

                    writer.append(escapeCsv(dateStr)).append(",")
                    writer.append(escapeCsv(timeStr)).append(",")
                    writer.append(escapeCsv(amountInr)).append(",")
                    writer.append(tx.amountPaise.toString()).append(",")
                    writer.append(escapeCsv(tx.status.name)).append(",")
                    writer.append(escapeCsv(tx.paymentMethod.displayName)).append(",")
                    writer.append(escapeCsv(tx.paymentId)).append(",")
                    writer.append(escapeCsv(tx.orderId)).append(",")
                    writer.append(escapeCsv(tx.eventId)).append(",")
                    writer.append(escapeCsv(tx.payerName ?: "")).append(",")
                    writer.append(escapeCsv(tx.source.name)).append(",")
                    writer.append(if (tx.isTest) "TRUE" else "FALSE").append("\n")
                }
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
