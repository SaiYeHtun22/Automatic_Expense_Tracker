package com.example.zeroclickexpense.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zeroclickexpense.data.Transaction
import com.example.zeroclickexpense.data.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class YearMonth(val year: Int, val month: Int)

enum class FilterMode {
    MONTHLY, CUSTOM_RANGE
}

class ExpenseViewModel(
    private val repository: TransactionRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "฿") ?: "฿")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _filterMode = MutableStateFlow(FilterMode.MONTHLY)
    val filterMode: StateFlow<FilterMode> = _filterMode.asStateFlow()

    private val _selectedWallet = MutableStateFlow("ALL")
    val selectedWallet: StateFlow<String> = _selectedWallet.asStateFlow()

    private val _selectedMonth = MutableStateFlow(run {
        val c = Calendar.getInstance()
        YearMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    })
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _customStartDate = MutableStateFlow(run {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -14) // default past 2 weeks
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    })
    val customStartDate: StateFlow<Long> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow(run {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        c.timeInMillis
    })
    val customEndDate: StateFlow<Long> = _customEndDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val displayedTransactions: Flow<List<Transaction>> = combine(
        _filterMode,
        _selectedMonth,
        _customStartDate,
        _customEndDate
    ) { mode, ym, start, end ->
        if (mode == FilterMode.MONTHLY) {
            Pair(getStartTime(ym), getEndTime(ym))
        } else {
            Pair(start, end)
        }
    }.flatMapLatest { (start, end) ->
        repository.getTransactionsBetween(start, end)
    }

    fun setCurrencySymbol(symbol: String) {
        _currencySymbol.value = symbol
        prefs.edit().putString("currency_symbol", symbol).apply()
        // Default wallet filter to all or current currency
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
    }

    fun setSelectedWallet(wallet: String) {
        _selectedWallet.value = wallet
    }

    fun setCustomDateRange(startDateMillis: Long, endDateMillis: Long) {
        val startCal = Calendar.getInstance().apply { 
            timeInMillis = startDateMillis 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply { 
            timeInMillis = endDateMillis 
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        _customStartDate.value = startCal.timeInMillis
        _customEndDate.value = endCal.timeInMillis
    }

    fun nextMonth() {
        val current = _selectedMonth.value
        _selectedMonth.value = if (current.month == 11) {
            YearMonth(current.year + 1, 0)
        } else {
            YearMonth(current.year, current.month + 1)
        }
    }

    fun previousMonth() {
        val current = _selectedMonth.value
        _selectedMonth.value = if (current.month == 0) {
            YearMonth(current.year - 1, 11)
        } else {
            YearMonth(current.year, current.month - 1)
        }
    }

    private fun getStartTime(ym: YearMonth): Long {
        val c = Calendar.getInstance()
        c.set(ym.year, ym.month, 1, 0, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getEndTime(ym: YearMonth): Long {
        val c = Calendar.getInstance()
        c.set(ym.year, ym.month, 1, 0, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.MONTH, 1)
        return c.timeInMillis - 1
    }

    fun insert(transaction: Transaction) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        repository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        repository.delete(transaction)
    }
}

class ExpenseViewModelFactory(
    private val repository: TransactionRepository,
    private val prefs: SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
