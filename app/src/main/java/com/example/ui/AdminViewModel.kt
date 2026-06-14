package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.TransactionRecord
import com.example.data.model.UserWallet
import com.example.data.repository.AdminRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AdminRepository(db.adminDao())

    // Live state streams
    val allWallets: StateFlow<List<UserWallet>> = repository.allWallets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionRecord>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state filters
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow("ALL") // ALL, DEPOSIT, WITHDRAWAL
    val selectedStatusFilter = MutableStateFlow("ALL") // ALL, PENDING, APPROVED, REJECTED

    // UI Feedback State
    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    // Active selected transaction for detailed operations (e.g. Reject / Approve modal)
    val selectedTransactionForAction = MutableStateFlow<TransactionRecord?>(null)

    // Combined filtered transactions
    val filteredTransactions: StateFlow<List<TransactionRecord>> = combine(
        allTransactions,
        searchQuery,
        selectedTypeFilter,
        selectedStatusFilter
    ) { txns, query, typeFilter, statusFilter ->
        txns.filter { record ->
            val matchesType = typeFilter == "ALL" || record.transactionType == typeFilter
            val matchesStatus = statusFilter == "ALL" || record.status == statusFilter
            val matchesQuery = query.isEmpty() ||
                    record.userName.contains(query, ignoreCase = true) ||
                    record.userId.contains(query, ignoreCase = true) ||
                    record.referenceId.contains(query, ignoreCase = true) ||
                    record.paymentMethod.contains(query, ignoreCase = true)

            matchesType && matchesStatus && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // High level metrics
    val dashboardMetrics: Flow<DashboardMetrics> = combine(allWallets, allTransactions) { wallets, txns ->
        val totalUserBalances = wallets.sumOf { it.balance }
        val approvedDeposits = txns.filter { it.transactionType == "DEPOSIT" && it.status == "APPROVED" }.sumOf { it.amount }
        val approvedWithdrawals = txns.filter { it.transactionType == "WITHDRAWAL" && it.status == "APPROVED" }.sumOf { it.amount }
        val pendingDeposits = txns.filter { it.transactionType == "DEPOSIT" && it.status == "PENDING" }.count()
        val pendingWithdrawals = txns.filter { it.transactionType == "WITHDRAWAL" && it.status == "PENDING" }.count()

        DashboardMetrics(
            totalSystemLiquidity = totalUserBalances,
            totalDepositsVolume = approvedDeposits,
            totalWithdrawalsVolume = approvedWithdrawals,
            pendingDepositsCount = pendingDeposits,
            pendingWithdrawalsCount = pendingWithdrawals,
            totalRegisteredUsers = wallets.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    init {
        viewModelScope.launch {
            // Seed database with default transactions if wallets are completely empty
            repository.allWallets.first().let { wallets ->
                if (wallets.isEmpty()) {
                    repository.checkAndSeedInitialData()
                }
            }
        }
    }

    fun approveTransaction(id: Int, note: String) {
        viewModelScope.launch {
            try {
                val outcome = repository.approveTransaction(id, note.ifBlank { "Actioned by Administrator" })
                _uiMessage.value = UiMessage.Success(outcome)
                selectedTransactionForAction.value = null
            } catch (e: Exception) {
                _uiMessage.value = UiMessage.Error(e.message ?: "Failed to approve transaction")
            }
        }
    }

    fun rejectTransaction(id: Int, note: String) {
        viewModelScope.launch {
            try {
                val outcome = repository.rejectTransaction(id, note.ifBlank { "Rejected by Administrator" })
                _uiMessage.value = UiMessage.Success(outcome)
                selectedTransactionForAction.value = null
            } catch (e: Exception) {
                _uiMessage.value = UiMessage.Error(e.message ?: "Failed to reject transaction")
            }
        }
    }

    // Simulations: Client request simulation (Deposit or Withdraw)
    fun simulateClientRequest(
        userId: String,
        type: String,
        amount: Double,
        method: String,
        details: String
    ) {
        viewModelScope.launch {
            val user = db.adminDao().getUserWalletById(userId)
            if (user == null) {
                _uiMessage.value = UiMessage.Error("Selected user does not exist.")
                return@launch
            }

            if (type == "WITHDRAWAL" && user.balance < amount) {
                _uiMessage.value = UiMessage.Error("Cannot simulate Withdrawal. User balance ($${String.format("%.2f", user.balance)}) is insufficient for $${String.format("%.2f", amount)}.")
                return@launch
            }

            val randomRef = "TXN-${(1000..9999).random()}-${(1000..9999).random()}"
            val newTxn = TransactionRecord(
                transactionType = type,
                userId = userId,
                userName = user.fullName,
                amount = amount,
                paymentMethod = method,
                paymentDetails = details.ifBlank { "Generic Address/Details" },
                referenceId = randomRef,
                status = "PENDING",
                timestamp = System.currentTimeMillis()
            )

            // Update user's active requests count
            val updatedUser = if (type == "DEPOSIT") {
                user.copy(activeDepositRequests = user.activeDepositRequests + 1)
            } else {
                user.copy(activeWithdrawRequests = user.activeWithdrawRequests + 1)
            }

            db.adminDao().insertTransaction(newTxn)
            db.adminDao().insertUserWallet(updatedUser)

            _uiMessage.value = UiMessage.Success(
                "Simulated new client $type request for $${String.format("%.2f", amount)} under Ref: $randomRef"
            )
        }
    }

    // Simulations: Admin creating a new user wallet
    fun createNewUserWallet(name: String, email: String, initialBalance: Double) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank()) {
                _uiMessage.value = UiMessage.Error("Name and Email are required.")
                return@launch
            }
            val randomId = "USR-${(100..999).random()}"
            val newUser = UserWallet(
                userId = randomId,
                fullName = name,
                email = email,
                balance = initialBalance
            )
            repository.insertWallet(newUser)
            _uiMessage.value = UiMessage.Success("Successfully created new user $name (ID: $randomId) with balance $${String.format("%.2f", initialBalance)}.")
        }
    }

    // Reset database
    fun resetDatabaseToDefaults() {
        viewModelScope.launch {
            repository.resetDatabase()
            _uiMessage.value = UiMessage.Success("Database successfully reset to initial default state!")
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }
}

// Support structs
sealed interface UiMessage {
    data class Success(val message: String) : UiMessage
    data class Error(val message: String) : UiMessage
}

data class DashboardMetrics(
    val totalSystemLiquidity: Double = 0.0,
    val totalDepositsVolume: Double = 0.0,
    val totalWithdrawalsVolume: Double = 0.0,
    val pendingDepositsCount: Int = 0,
    val pendingWithdrawalsCount: Int = 0,
    val totalRegisteredUsers: Int = 0
)
