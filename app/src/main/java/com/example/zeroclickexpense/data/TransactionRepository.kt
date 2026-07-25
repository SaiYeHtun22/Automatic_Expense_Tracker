package com.example.zeroclickexpense.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetween(startDate, endDate)
    }

    fun getTotalExpenseBetween(startDate: Long, endDate: Long): Flow<Double?> {
        return transactionDao.getTotalExpenseBetween(startDate, endDate)
    }

    fun getTotalIncomeBetween(startDate: Long, endDate: Long): Flow<Double?> {
        return transactionDao.getTotalIncomeBetween(startDate, endDate)
    }
}
