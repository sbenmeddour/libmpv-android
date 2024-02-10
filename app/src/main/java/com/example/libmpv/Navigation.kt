package com.example.libmpv

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

val LocalNavController = staticCompositionLocalOf<NavController> { error("") }

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun Navigator() {
  val controller = rememberNavController()
  CompositionLocalProvider(
    LocalNavController provides controller,
  ) {
    NavHost(
      navController = controller,
      startDestination = "home",
      builder = {
        composable(
          route = "home",
          content = {
            HomeScreen()
          }
        )
        composable(
          route = "player/{url}",
          content = {
            val encodedString = it.arguments!!.getString("url") ?: throw NullPointerException("Cannot find URL in arguments")
            val decodedUrl = Base64.decode(encodedString)
            PlayerScreen(url = decodedUrl.decodeToString())
          }
        )
      }
    )
  }


}