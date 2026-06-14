package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_records")
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val transactionType: String, // "DEPOSIT" or "WITHDRAWAL"
    val userId: String,
    val userName: String,
    val amount: Double,
    val paymentMethod: String, // e.g. "USDT (TRC-20)", "Bank Transfer", "PayPal", "Visa/Mastercard"
    val paymentDetails: String, // bank account number or crypto wallet address
    val referenceId: String, // transaction hash or bank receipt reference
    val status: String, // "PENDING", "APPROVED", "REJECTED"
    val timestamp: Long = System.currentTimeMillis(),
    val adminNote: String? = null
)
