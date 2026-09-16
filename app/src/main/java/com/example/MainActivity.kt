package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainCollageScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CollageViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .border(width = 6.dp, color = Color(0xFF56B4FD)), // Sky blue and bold outer border
          color = MaterialTheme.colorScheme.background
        ) {
          val viewModel: CollageViewModel = viewModel()
          MainCollageScreen(viewModel = viewModel)
        }
      }
    }
  }
}
