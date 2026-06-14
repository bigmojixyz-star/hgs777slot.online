package com.example.data.repository

import com.example.data.dao.AdminDao
import com.example.data.model.TransactionRecord
import com.example.data.model.UserWallet
import kotlinx.coroutines.flow.Flow

class AdminRepository(private val adminDao: AdminDao) {

    val allWallets: Flow<List<UserWallet>> = adminDao.getAllUserWallets()
    val allTransactions: Flow<List<TransactionRecord>> = adminDao.getAllTransactions()

    suspend fun insertWallet(userWallet: UserWallet) {
        adminDao.insertUserWallet(userWallet)
    }

    suspend fun insertTransaction(record: TransactionRecord) {
        adminDao.insertTransaction(record)
    }

    /**
     * Approves a transaction. Updates user balance accordingly.
     * Returns a string message indicating success, or throws exception if error occurs.
     */
    suspend fun approveTransaction(id: Int, note: String?): String {
        val record = adminDao.getTransactionById(id) ?: return "Transaction not found."
        if (record.status != "PENDING") {
            return "Transaction is already processed (Status: ${record.status})"
        }

        val user = adminDao.getUserWalletById(record.userId)
            ?: return "User matching this request (${record.userId}) does not exist."

        val updatedRecord = record.copy(status = "APPROVED", adminNote = note)

        if (record.transactionType == "DEPOSIT") {
            // Add user balance
            val newBalance = user.balance + record.amount
            val updatedUser = user.copy(
                balance = newBalance,
                activeDepositRequests = (user.activeDepositRequests - 1).coerceAtLeast(0)
            )
            adminDao.updateTransaction(updatedRecord)
            adminDao.insertUserWallet(updatedUser)
            return "Deposit of $${String.format("%.2f", record.amount)} approved. User balance is now $${String.format("%.2f", newBalance)}."
        } else {
            // WITHDRAWAL
            // Check if user has sufficient funds
            if (user.balance < record.amount) {
                throw IllegalStateException("Insufficient balance. User has $${String.format("%.2f", user.balance)} but requested withdrawal of $${String.format("%.2f", record.amount)}.")
            }
            // Deduct user balance
            val newBalance = user.balance - record.amount
            val updatedUser = user.copy(
                balance = newBalance,
                activeWithdrawRequests = (user.activeWithdrawRequests - 1).coerceAtLeast(0)
            )
            adminDao.updateTransaction(updatedRecord)
            adminDao.insertUserWallet(updatedUser)
            return "Withdrawal of $${String.format("%.2f", record.amount)} approved. User balance is now $${String.format("%.2f", newBalance)}."
        }
    }

    /**
     * Rejects a transaction.
     */
    suspend fun rejectTransaction(id: Int, note: String?): String {
        val record = adminDao.getTransactionById(id) ?: return "Transaction not found."
        if (record.status != "PENDING") {
            return "Transaction is already processed (Status: ${record.status})"
        }

        val user = adminDao.getUserWalletById(record.userId)
        val updatedRecord = record.copy(status = "REJECTED", adminNote = note)

        if (user != null) {
            val updatedUser = if (record.transactionType == "DEPOSIT") {
                user.copy(activeDepositRequests = (user.activeDepositRequests - 1).coerceAtLeast(0))
            } else {
                user.copy(activeWithdrawRequests = (user.activeWithdrawRequests - 1).coerceAtLeast(0))
            }
            adminDao.insertUserWallet(updatedUser)
        }

        adminDao.updateTransaction(updatedRecord)
        return "Transaction ${record.referenceId} rejected successfully."
    }

    /**
     * Seeds initial default transactions & wallets if they are empty
     */
    suspend fun checkAndSeedInitialData() {
        val initialWallets = listOf(
            UserWallet("USR-101", "Alex Vance", "alex.v@website.com", 1450.00, 1, 0),
            UserWallet("USR-102", "Elena Rostova", "elena.r@website.com", 85.50, 0, 1),
            UserWallet("USR-103", "Marcus Broady", "marcus.b@website.com", 4200.00, 1, 1),
            UserWallet("USR-104", "Kenji Sato", "kenji.s@website.com", 12450.00, 0, 0),
            UserWallet("USR-105", "Sophia Martinez", "sophia.m@website.com", 0.00, 1, 0)
        )

        // Seed wallets if empty
        // To verify if empty, we just insert them (can do check first)
        adminDao.insertUserWallets(initialWallets)

        val initialTxns = listOf(
            TransactionRecord(
                id = 1,
                transactionType = "DEPOSIT",
                userId = "USR-101",
                userName = "Alex Vance",
                amount = 500.00,
                paymentMethod = "USDT (TRC-20)",
                paymentDetails = "TL1p7uSGeY7bXm...KWe5Xg",
                referenceId = "TXN-9021-8201",
                status = "PENDING",
                timestamp = System.currentTimeMillis() - 3600000 * 2 // 2 hours ago
            ),
            TransactionRecord(
                id = 2,
                transactionType = "WITHDRAWAL",
                userId = "USR-102",
                userName = "Elena Rostova",
                amount = 250.00,
                paymentMethod = "Bank Transfer",
                paymentDetails = "Chase Bank - Acct: ***8821",
                referenceId = "TXN-8819-3329",
                status = "PENDING",
                timestamp = System.currentTimeMillis() - 3600000 * 4 // 4 hours ago
            ),
            TransactionRecord(
                id = 3,
                transactionType = "DEPOSIT",
                userId = "USR-103",
                userName = "Marcus Broady",
                amount = 2500.00,
                paymentMethod = "USDT (TRC-20)",
                paymentDetails = "TXoY8a9SFeY7bXm...WreQuZ",
                referenceId = "TXN-7153-4819",
                status = "PENDING",
                timestamp = System.currentTimeMillis() - 3600000 * 12 // 12 hours ago
            ),
            TransactionRecord(
                id = 4,
                transactionType = "WITHDRAWAL",
                userId = "USR-103",
                userName = "Marcus Broady",
                amount = 300.00,
                paymentMethod = "PayPal",
                paymentDetails = "marcus.b@website.com",
                referenceId = "TXN-6617-9102",
                status = "PENDING",
                timestamp = System.currentTimeMillis() - 3600000 * 14 // 14 hours ago
            ),
            TransactionRecord(
                id = 5,
                transactionType = "DEPOSIT",
                userId = "USR-104",
                userName = "Kenji Sato",
                amount = 10000.00,
                paymentMethod = "USDT (TRC-20)",
                paymentDetails = "TE9ZgT7bXm7uSG...Y6u5Xg",
                referenceId = "TXN-1193-2041",
                status = "APPROVED",
                timestamp = System.currentTimeMillis() - 3600000 * 24, // 1 day ago
                adminNote = "Verified blockchain hash. Funds confirmed in system ledger wallet."
            ),
            TransactionRecord(
                id = 6,
                transactionType = "WITHDRAWAL",
                userId = "USR-101",
                userName = "Alex Vance",
                amount = 150.00,
                paymentMethod = "PayPal",
                paymentDetails = "alex.v@website.com",
                referenceId = "TXN-4012-3381",
                status = "APPROVED",
                timestamp = System.currentTimeMillis() - 3600000 * 25,
                adminNote = "Sent via PayPal. Transaction ID in PayPal console: PP-7782A"
            ),
            TransactionRecord(
                id = 7,
                transactionType = "DEPOSIT",
                userId = "USR-105",
                userName = "Sophia Martinez",
                amount = 100.00,
                paymentMethod = "Visa/Mastercard",
                paymentDetails = "Card ending in *4412",
                referenceId = "TXN-2212-0941",
                status = "PENDING",
                timestamp = System.currentTimeMillis() - 3600000 * 30
            ),
            TransactionRecord(
                id = 8,
                transactionType = "WITHDRAWAL",
                userId = "USR-102",
                userName = "Elena Rostova",
                amount = 1200.00,
                paymentMethod = "Bank Transfer",
                paymentDetails = "Chase Bank - Acct: ***8821",
                referenceId = "TXN-1122-3847",
                status = "REJECTED",
                timestamp = System.currentTimeMillis() - 3600000 * 48,
                adminNote = "Rejected due to insufficient balance. Elena has only $85.50 in wallet."
            )
        )

        initialTxns.forEach { txn ->
            adminDao.insertTransaction(txn)
        }
    }

    suspend fun resetDatabase() {
        adminDao.deleteAllUserWallets()
        adminDao.deleteAllTransactions()
        checkAndSeedInitialData()
    }
}
