package io.agora.agorapttdemo.utilities

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import io.agora.agorapttdemo.viewmodels.PTTState
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.ref.WeakReference

interface Channel {
    fun startTalk()

    fun stopTalk()
}


private val tag = "Channel"
class HotChannel(val voiceManager: VoiceManager, val pttState: MutableLiveData<PTTState>): Channel {
    init {
        voiceManager.goHot {  }

    }

    override fun startTalk() {
        voiceManager.broadcast(true)
    }

    override fun stopTalk() {
        voiceManager.broadcast(false)
    }
}


class ColdChannel: Channel {
    override fun startTalk() {
        TODO("Not yet implemented")
    }

    override fun stopTalk() {
        TODO("Not yet implemented")
    }
}