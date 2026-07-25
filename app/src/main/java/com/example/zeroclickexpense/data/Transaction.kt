package com.example.zeroclickexpense.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String, // e.g., "Food", "Transport", "Unknown"
    val merchant: String, // e.g., "Starbucks"
    val source: String, // e.g., "Chase Bank", "Voice"
    val date: Long, // timestamp
    val rawText: String? = null, // the original notification text or voice transcript
    val isIncome: Boolean = false,
    val currency: String = "฿" // per-transaction currency symbol: ฿, Ks, $, €, £, ¥, ₩, ₹, RM
)
