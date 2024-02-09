package com.example.libmpv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.libmpv.ui.theme.AppTheme
import fr.nextv.libmpv.LibMpv

class MainActivity : ComponentActivity() {

  init {
    LibMpv.initializeLibMpv()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        Navigator()
      }
    }
  }

}

