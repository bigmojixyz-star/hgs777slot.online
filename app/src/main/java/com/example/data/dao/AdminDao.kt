package com.example.data.dao

import androidx.room.*
import com.example.data.model.TransactionRecord
import com.example.data.model.UserWallet
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDao {

    // --- User Wallet Operations ---
    @Query("SELECT * FROM user_wallets ORDER BY userId ASC")
    fun getAllUserWallets(): Flow<List<UserWallet>>

    @Query("SELECT * FROM user_wallets WHERE userId = :userId")
    suspend fun getUserWalletById(userId: String): UserWallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserWallet(userWallet: UserWallet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserWallets(userWallets: List<UserWallet>)


    // --- Transaction Record Operations ---
    @Query("SELECT * FROM transaction_records ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transaction_records WHERE id = :id")
    suspend fun getTransactionById(id: Int): TransactionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(record: TransactionRecord)

    @Update
    suspend fun updateTransaction(record: TransactionRecord)

    // Helper to delete all records for resetting values
    @Query("DELETE FROM user_wallets")
    suspend fun deleteAllUserWallets()

    @Query("DELETE FROM transaction_records")
    suspend fun deleteAllTransactions()
}
