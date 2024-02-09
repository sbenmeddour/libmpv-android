package com.example.libmpv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun HomeScreen() {
  val navController = LocalNavController.current
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Libmpv-android test app") }
      )
    },
  ) { scaffoldPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(scaffoldPadding),
      verticalArrangement = Arrangement.SpaceAround,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Button(
        onClick = {
          val url = "https://github.com/ietf-wg-cellar/matroska-test-files/raw/master/test_files/test5.mkv"
          val encodedUrl = Base64.encode(url.toByteArray())
          navController.navigate("player/$encodedUrl")
        },
        content = {
          Text(text = "Elephant")
        }
      )
    }
  }

}