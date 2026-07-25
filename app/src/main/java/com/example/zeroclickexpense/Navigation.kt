package com.example.zeroclickexpense

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.zeroclickexpense.ui.DashboardScreen
import com.example.zeroclickexpense.viewmodel.ExpenseViewModel
import com.example.zeroclickexpense.viewmodel.ExpenseViewModelFactory

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)
  val context = LocalContext.current
  val application = context.applicationContext as ZeroClickApplication
  val prefs = context.getSharedPreferences("ZeroTrackPrefs", Context.MODE_PRIVATE)
  
  val viewModel: ExpenseViewModel = viewModel(
    factory = ExpenseViewModelFactory(application.repository, prefs)
  )

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          DashboardScreen(viewModel = viewModel)
        }
      },
  )
}
