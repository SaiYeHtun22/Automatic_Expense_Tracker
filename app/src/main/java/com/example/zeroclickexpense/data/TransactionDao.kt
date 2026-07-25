package com.example.zeroclickexpense.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    fun insert(transaction: Transaction)

    @Update
    fun update(transaction: Transaction)

    @Delete
    fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE date >= :startDate AND date <= :endDate AND isIncome = 0")
    fun getTotalExpenseBetween(startDate: Long, endDate: Long): Flow<Double?>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE date >= :startDate AND date <= :endDate AND isIncome = 1")
    fun getTotalIncomeBetween(startDate: Long, endDate: Long): Flow<Double?>
}
