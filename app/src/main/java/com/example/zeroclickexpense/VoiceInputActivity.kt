package com.example.zeroclickexpense

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.zeroclickexpense.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceInputActivity : ComponentActivity() {

    private var speechRecognizer: SpeechRecognizer? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No setContentView, making it appear transparent via theme

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.e("VoiceInput", "Error code: $error")
                Toast.makeText(this@VoiceInputActivity, "Voice recognition failed", Toast.LENGTH_SHORT).show()
                finish()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    processVoiceInput(spokenText)
                } else {
                    finish()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }

        speechRecognizer?.startListening(intent)
        Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show()
    }

    private fun processVoiceInput(text: String) {
        // e.g. "Spent 20 dollars on lunch"
        // Super simple parser: look for number
        val amountRegex = Regex("(\\d+(\\.\\d{1,2})?)")
        val match = amountRegex.find(text)

        if (match != null) {
            val amount = match.groupValues[1].toDoubleOrNull()
            if (amount != null) {
                // Determine currency from spoken text
                val lowerText = text.lowercase()
                val detectedCurrency = when {
                    lowerText.contains("kyat") || lowerText.contains("ks") || lowerText.contains("mmk") || lowerText.contains("ကျပ်") -> "Ks"
                    lowerText.contains("baht") || lowerText.contains("thb") || lowerText.contains("บาท") -> "฿"
                    lowerText.contains("dollar") || lowerText.contains("usd") -> "$"
                    lowerText.contains("euro") -> "€"
                    lowerText.contains("pound") -> "£"
                    lowerText.contains("yen") -> "¥"
                    lowerText.contains("won") -> "₩"
                    lowerText.contains("rupee") -> "₹"
                    lowerText.contains("ringgit") || lowerText.contains("rm") -> "RM"
                    else -> getSharedPreferences("ZeroTrackPrefs", android.content.Context.MODE_PRIVATE).getString("currency_symbol", "฿") ?: "฿"
                }

                saveTransaction(amount, detectedCurrency, text)
            } else {
                Toast.makeText(this, "Could not parse amount from: $text", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            Toast.makeText(this, "No amount found in: $text", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun saveTransaction(amount: Double, currency: String, rawText: String) {
        lifecycleScope.launch {
            val app = application as ZeroClickApplication
            val transaction = Transaction(
                amount = amount,
                currency = currency,
                category = "Voice Input",
                merchant = "Unknown",
                source = "Voice",
                date = System.currentTimeMillis(),
                rawText = rawText
            )
            withContext(Dispatchers.IO) {
                app.repository.insert(transaction)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@VoiceInputActivity, "Added $currency$amount (Heard: \"$rawText\")", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
