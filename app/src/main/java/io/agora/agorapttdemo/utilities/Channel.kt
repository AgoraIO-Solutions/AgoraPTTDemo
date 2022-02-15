package io.agora.agorapttdemo.utilities

import android.util.Log

interface Channel {
    fun pttToggle()
}

private val tag = "Channel"
class HotChannel: Channel {
    override fun pttToggle() {
        Log.i(tag , "Hot Channel Toggle")
    }
}

class ColdChannel: Channel {
    override fun pttToggle() {
        Log.i(tag , "Cold Channel Toggle")
    }
}