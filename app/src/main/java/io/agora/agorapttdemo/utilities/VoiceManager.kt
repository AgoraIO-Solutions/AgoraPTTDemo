package io.agora.agorapttdemo.utilities

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.agora.agorapttdemo.R
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
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

private val tag = "VoiceManager"
class VoiceManager @Inject constructor(val config: Config, context: Context): IRtcEngineEventHandler()  {
    private lateinit var rtcEngine: RtcEngine
    //var currentMessage: Message? = null
    private var hotCallback: (() -> Unit) ? = null
    init {
        try {
            rtcEngine = RtcEngine.create(context, config.appId, this)
        } catch (th: Throwable) {
            Log.i(tag, "Error initializing engine $th")
        }
        rtcEngine.muteLocalAudioStream(true)
        rtcEngine.setEnableSpeakerphone(true)
    }

    fun broadcast(boolean: Boolean) {
        rtcEngine.muteLocalAudioStream(!boolean)
    }

    fun goHot(callback: () -> Unit) {
        hotCallback = callback
        rtcEngine.joinChannel(config.tempToken, config.channel, "", 0)
    }

    fun goCold() {
        rtcEngine.leaveChannel()
    }

    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        Log.i(tag, "successfully joined channel with UID $uid")
        hotCallback?.invoke()
    }

    override fun onFirstLocalAudioFramePublished(elapsed: Int) {
       // TL.i("${currentMessage?.i ?: ""} onFirstLocalAudioFramePublished: Audio sent")
        Log.i(tag, "onFirstLocalAudioFramePublished")
    }

    override fun onFirstRemoteAudioDecoded(uid: Int, elapsed: Int) {
        //TL.i("\"${currentMessage?.i ?: ""} onFirstRemoteAudioDecoded: Received First Audio Frame")
        Log.i(tag, "onFirstRemoteAudioDecoded")
    }

    override fun onFirstRemoteAudioFrame(uid: Int, elapsed: Int) {
        //TL.i("\"${currentMessage?.i ?: ""} onFirstRemoteAudioFrame: Received First Audio Frame")
        Log.i(tag, "onFirstRemoteAudioFrame")
    }

    override fun onRemoteAudioStats(stats: IRtcEngineEventHandler.RemoteAudioStats?) {
        //TL.i("\"${currentMessage?.i ?: ""} onRemoteAudioStats ${stats?.networkTransportDelay ?: 0L}")
        Log.i(tag, "onRemoteAudioStats")
    }

    override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
        //TL.i("onRemoteAudioStateChanged uid $uid, state $state reason $reason elapsed $elapsed")
        Log.i(tag, "onRemoteAudioStateChanged")
    }

    fun destroy() {
        rtcEngine.leaveChannel()
        RtcEngine.destroy()
    }
}