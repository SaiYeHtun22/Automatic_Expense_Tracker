package com.example.zeroclickexpense.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.zeroclickexpense.ZeroClickApplication
import com.example.zeroclickexpense.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            val extras = it.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            Log.d("ZeroTrackListener", "Received from $packageName: $title - $text")

            // Filter LINE messages to ensure we only parse bank official bots like Krungthai Connext or KBZPay
            if (packageName == "jp.naver.line.android" && 
                !title.contains("Connext", ignoreCase = true) && 
                !title.contains("Krungthai", ignoreCase = true) &&
                !title.contains("SCB", ignoreCase = true) &&
                !title.contains("K PLUS", ignoreCase = true) &&
                !title.contains("KBZ", ignoreCase = true) &&
                !title.contains("Bank", ignoreCase = true)) {
                return
            }

            val prefs = applicationContext.getSharedPreferences("ZeroTrackPrefs", Context.MODE_PRIVATE)
            val defaultCurrency = prefs.getString("currency_symbol", "฿") ?: "฿"

            val combinedText = "$title $text".lowercase()

            // 1. Determine Currency (Multi-Wallet auto-detection)
            val transactionCurrency = when {
                combinedText.contains("ks") || combinedText.contains("mmk") || combinedText.contains("kyat") || combinedText.contains("kbz") || combinedText.contains("aya bank") || combinedText.contains("cb bank") -> "Ks"
                combinedText.contains("thb") || combinedText.contains("baht") || combinedText.contains("บาท") || combinedText.contains("k plus") || combinedText.contains("krungthai") -> "฿"
                combinedText.contains("usd") || combinedText.contains("$") || combinedText.contains("dollar") || combinedText.contains("sgd") || combinedText.contains("aud") || combinedText.contains("cad") -> "$"
                combinedText.contains("eur") || combinedText.contains("€") || combinedText.contains("euro") -> "€"
                combinedText.contains("gbp") || combinedText.contains("£") || combinedText.contains("pound") -> "£"
                combinedText.contains("jpy") || combinedText.contains("cny") || combinedText.contains("¥") || combinedText.contains("yen") || combinedText.contains("yuan") -> "¥"
                combinedText.contains("krw") || combinedText.contains("₩") || combinedText.contains("won") -> "₩"
                combinedText.contains("inr") || combinedText.contains("₹") || combinedText.contains("rupee") -> "₹"
                combinedText.contains("myr") || combinedText.contains("rm") || combinedText.contains("ringgit") -> "RM"
                else -> defaultCurrency
            }

            var amount: Double? = null
            var merchantName: String = if (title.isNotBlank()) title else "Card / Transfer"
            var isIncome = false

            // Bangkok Bank format
            val bkkRegex = Regex("paid\\s*([\\d,]+\\.?\\d*)\\s*THB", RegexOption.IGNORE_CASE)
            val bkkMatch = bkkRegex.find(text)

            // Krungthai Connext format: "Transferred: -500.00 THB from account..."
            val ktbRegex = Regex("Transferred:\\s*-?([\\d,]+\\.?\\d*)\\s*(?:THB|Baht)", RegexOption.IGNORE_CASE)
            val ktbMatch = ktbRegex.find(text) ?: Regex("-([\\d,]+\\.?\\d*)\\s*(?:THB|Baht|Ks|MMK|\\$|EUR|GBP)").find(text)

            // K PLUS format
            val kbankRegex = Regex("Amount\\s+([\\d,]+\\.\\d{2})\\s+Baht", RegexOption.IGNORE_CASE)
            val kbankMatch = kbankRegex.find(text)

            // Worldwide Universal Currency Regex (Matches XX.XX THB, Baht, บาท, Ks, MMK, $, EUR, GBP, JPY, KRW, MYR, INR, RM)
            val universalAfterRegex = Regex("([\\d,]+\\.\\d{2})\\s*(?:THB|Baht|บาท|Ks|MMK|USD|EUR|GBP|JPY|KRW|INR|MYR|\\$|€|£|¥|₩|₹|RM)", RegexOption.IGNORE_CASE)
            val universalBeforeRegex = Regex("(?:THB|Baht|บาท|Ks|MMK|USD|EUR|GBP|JPY|KRW|INR|MYR|\\$|€|£|¥|₩|₹|RM)\\s*([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)
            
            // Universal Worldwide Action Keywords (spent, paid, debited, charged, deposit, received, purchase)
            val actionKeywordRegex = Regex("(?:spent|paid|debited|charged|purchase of|transfer of|deposit of|received)\\s*([\\d,]+\\.?\\d*)", RegexOption.IGNORE_CASE)

            val universalMatch = universalAfterRegex.find(text) ?: universalBeforeRegex.find(text) ?: actionKeywordRegex.find(text)

            if (bkkMatch != null) {
                amount = bkkMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                val merchantMatch = Regex("to\\s+(.*?)\\s+at\\s+\\d", RegexOption.IGNORE_CASE).find(text)
                if (merchantMatch != null) {
                    merchantName = merchantMatch.groupValues[1].trim()
                } else {
                    merchantName = "Bangkok Bank"
                }
            } else if (ktbMatch != null) {
                amount = ktbMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                merchantName = "Krungthai Connext"
                isIncome = false
            } else if (kbankMatch != null) {
                amount = kbankMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                merchantName = "K PLUS"
            } else if (universalMatch != null) {
                amount = universalMatch.groupValues[1].replace(",", "").toDoubleOrNull()
            }

            // Check if amount was extracted
            if (amount != null && amount > 0) {
                val repository = (applicationContext as ZeroClickApplication).repository

                // Global keywords to detect Income vs Expense
                val incomeKeywords = listOf("deposit", "receive", "received", "credited", "inflow", "transfer from", "รับโอน", "เงินเข้า")
                if (incomeKeywords.any { combinedText.contains(it) }) {
                    isIncome = true
                }

                val transaction = Transaction(
                    amount = amount,
                    category = if (isIncome) "Income" else "Expense",
                    merchant = merchantName,
                    source = packageName,
                    date = System.currentTimeMillis(),
                    rawText = "$title - $text",
                    isIncome = isIncome,
                    currency = transactionCurrency
                )

                CoroutineScope(Dispatchers.IO).launch {
                    repository.insert(transaction)
                    Log.d("ZeroTrackListener", "Saved transaction: $amount $transactionCurrency ($merchantName) from $packageName")
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
