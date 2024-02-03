package fr.nextv.libmpv

class NativeLib {

    /**
     * A native method that is implemented by the 'libmpv' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'libmpv' library on application startup.
        init {
            System.loadLibrary("libmpv")
        }
    }
}