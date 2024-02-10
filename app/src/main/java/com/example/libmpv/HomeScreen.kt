package com.example.libmpv

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
      verticalArrangement = Arrangement.spacedBy(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(modifier = Modifier.weight(1f))
      Button(
        onClick = {
          val url = "https://github.com/ietf-wg-cellar/matroska-test-files/raw/master/test_files/test5.mkv"
          val encodedUrl = Base64.encode(url.toByteArray())
          navController.navigate("player/$encodedUrl")
        },
        content = {
          Text(text = "Elephant dreams")
        }
      )
      Button(
        onClick = {
          val url = "https://lafibre.info/videos/test/201411_blender_big_buck_bunny_24fps_1080p_hevc.mp4"
          val encodedUrl = Base64.encode(url.toByteArray())
          navController.navigate("player/$encodedUrl")
        },
        content = {
          Text(text = "Bunny 1080p HEVC (H.265)")
        }
      )
      Spacer(modifier = Modifier.weight(1f))
    }
  }

}