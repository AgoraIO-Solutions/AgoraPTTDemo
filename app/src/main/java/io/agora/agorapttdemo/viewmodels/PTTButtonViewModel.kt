package io.agora.agorapttdemo.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.agorapttdemo.utilities.*
import javax.inject.Inject

enum class PTTState {
    RECEIVING, CONNECTING, BROADCASTING, INACTIVE
}

private val tag = "PTTButtonViewModel"

@HiltViewModel
class PTTButtonViewModel @Inject constructor(
    @ConnectingSound val connectingSound: Alerter,
    @SendingSound val sendingSound: Alerter,
    @ReceivingSound val receivingSound: Alerter,
    private val voiceManager: VoiceManager,
    private val channelLock: ChannelLock
): ViewModel() {
    enum class ChannelType {
        COLD, HOT
    }

    private var channel: Channel

    val channelType: MutableLiveData<ChannelType> = MutableLiveData(ChannelType.HOT)
    val alertsOn: MutableLiveData<Boolean> = MutableLiveData(true)
    var state: MutableLiveData<PTTState> = MutableLiveData(PTTState.INACTIVE)

    init {
        channelType.observeForever { onChanged(it) }
        channelLock.stateData.observeForever { onChanged(it) }
        channel = HotChannel(voiceManager = voiceManager, pttState = state)
    }

    private fun onChanged(t: ChannelType) {
        Log.i(tag, "changing channel to $t")
        channel = when(t) {
            ChannelType.COLD ->  ColdChannel()
            ChannelType.HOT -> HotChannel(voiceManager = voiceManager, pttState = state)
        }
    }

    private fun onChanged(t: ChannelLock.State) {
        Log.i(tag, "changing channel to $t")
        when(t) {
            ChannelLock.State.LOCKED -> {
                state.value = PTTState.BROADCASTING
                connectingSound.stop()
                startTalking()
            }
            ChannelLock.State.LOCKED_BY_OTHER -> {
                state.value = PTTState.RECEIVING
                if (alertsOn.value == true) {
                    receivingSound.play { /* noop */ }
                }
            }
            ChannelLock.State.LOCKING -> {
                state.value = PTTState.CONNECTING
                connectingSound.play { /* noop */ }
            }
            ChannelLock.State.UNLOCKED -> {
                state.value = PTTState.INACTIVE
                connectingSound.stop()
            }
        }
    }

    private fun startTalking() {
        Log.i(tag, "should start talking")
        if (alertsOn.value == true) {
            sendingSound.play {
                channel.startTalk()
            }
        } else {
            channel.startTalk()
        }
    }

    fun pttPushed() {
        Log.i(tag, "PttButton pushed")

        if (state.value != PTTState.INACTIVE) return
        channelLock.acquireLock()
    }

    fun pttStop() {
        Log.i(tag, "PttButton 'Un' pushed")
        if (state.value != PTTState.CONNECTING && state.value != PTTState.BROADCASTING) return
        channel.stopTalk()
        channelLock.releaseLock()
    }
}