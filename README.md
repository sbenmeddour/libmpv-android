## libmpv for android

# based on
- [libmpv-android](https://github.com/jarnedemeulemeester/libmpv-android)
- [mpv-android](https://github.com/mpv-android/mpv-android)

# TODO:
- Stability
- Add vulkan support
- Http headers
- Drm
- Improve api...

```kotlin
@Composable
fun Example() {
  val player = remember {
    val config = MpvConfiguration()
    MpvPlayer(config)
  }
  val callback = remember {
    object : SurfaceHolder.Callback {
      override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        player.onSurfaceSizeChanged(width, height)
      }
      override fun surfaceCreated(holder: SurfaceHolder) = player.onSurfaceCreated(holder.surface)
      override fun surfaceDestroyed(holder: SurfaceHolder) = player.onSurfaceDestroyed()
    }
  }
  DisposableEffect(Unit) {
    player.command("loadfile {your file url}")
    onDispose {
      player.stop()
      player.release()
    }
  }
  AndroidView(
    factory = {
      SurfaceView(it).apply {
        holder.addCallback(callback)
      }
    },
    onRelease = {
      it.holder.removeCallback(callback)
    }
  )
}

```
