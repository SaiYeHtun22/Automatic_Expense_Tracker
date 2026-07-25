package com.example.zeroclickexpense

import android.app.Application
import com.example.zeroclickexpense.data.AppDatabase
import com.example.zeroclickexpense.data.TransactionRepository

class ZeroClickApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TransactionRepository(database.transactionDao()) }
}
