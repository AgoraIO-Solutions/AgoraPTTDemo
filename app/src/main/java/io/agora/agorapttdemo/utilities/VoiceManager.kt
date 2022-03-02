package io.agora.agorapttdemo.utilities

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.*
import org.joda.time.DateTime
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class VoiceManagerModule {
    @Provides
    @Singleton
    fun voiceManager(config: Config, @ApplicationContext context: Context): VoiceManager {
        return VoiceManager(config, context)
    }
}

private const val tag = "VoiceManager"
class VoiceManager @Inject constructor(val config: Config, context: Context): IRtcEngineEventHandler()  {
    private lateinit var rtcEngine: RtcEngine
    var isHot: Boolean = true

    private val disconnectAfterTime: Int get() = if (isHot) config.hotChannelTimeout else config.coldChannelTimeout

    private var keepAliveJob: Job? = null
    private var lastActivity: DateTime = DateTime.now()
    private var isConnected: Boolean = false
    private var isBroadcasting = false
    private var connectionCallback: (() -> Unit) ? = null
    init {
        try {
            rtcEngine = RtcEngine.create(context, config.appId, this)
        } catch (th: Throwable) {
            Log.i(tag, "Error initializing engine $th")
        }
        rtcEngine.enableAudioVolumeIndication(1,3, true)
        rtcEngine.muteLocalAudioStream(true)
        rtcEngine.setEnableSpeakerphone(true)
    }

    fun broadcast(on:Boolean, callback: () -> Unit) {
        rtcEngine.muteLocalAudioStream(!on)
        isBroadcasting = on
        if (!on) {
            rtcEngine.leaveChannel()
            return callback()
        }
        if (isConnected) return callback()
        connectionCallback = callback
        connectToChannel()
    }

    private fun connectToChannel() {
        rtcEngine.joinChannel(config.tempToken, config.channel, "", 0)
    }

    fun listen(callback: () -> Unit) {
        isBroadcasting = false
        rtcEngine.muteLocalAudioStream(true)
        if (isConnected) return callback()
        connectionCallback = callback
        connectToChannel()
    }

    fun destroy() {
        rtcEngine.leaveChannel()
        RtcEngine.destroy()
    }

    // MARK: IRtcEngineEventHandler
    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        Log.i(tag, "successfully joined channel with UID $uid")
        lastActivity = DateTime.now()
        connectionCallback?.invoke()
        isConnected = true
        keepAliveJob = startKeepAliveTimer()
    }

    override fun onLeaveChannel(stats: RtcStats?) {
        Log.i(tag, "successfully left the channel")
        isConnected = false
        CoroutineScope(Dispatchers.IO).launch {
            keepAliveJob?.cancelAndJoin()
            keepAliveJob = null
        }
    }

    private fun startKeepAliveTimer(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(3 * 1000)
                Log.i(tag, "Checking to see if channel should disconnect")
                if (lastActivity.isBefore(DateTime.now().minusSeconds(disconnectAfterTime)) and !isBroadcasting) {
                    rtcEngine.leaveChannel()
                }
            }
        }
    }

    override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
        Log.i(tag, "onAudioVolume Indicator")
        lastActivity = DateTime.now()
    }
}
