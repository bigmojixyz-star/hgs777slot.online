package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_wallets")
data class UserWallet(
    @PrimaryKey
    val userId: String, // e.g. "USR-7402"
    val fullName: String,
    val email: String,
    val balance: Double, // current approved funds
    val activeDepositRequests: Int = 0,
    val activeWithdrawRequests: Int = 0
)
