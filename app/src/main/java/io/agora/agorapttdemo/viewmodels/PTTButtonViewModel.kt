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
    val voiceManager: VoiceManager,
): ViewModel(), Observer<PTTButtonViewModel.ChannelType> {
    enum class ChannelType {
        COLD, HOT
    }

    private var channel: Channel = HotChannel()

    val channelType: MutableLiveData<ChannelType> = MutableLiveData(ChannelType.HOT)
    val alertsOn: MutableLiveData<Boolean> = MutableLiveData(true)
    var state: MutableLiveData<PTTState> = MutableLiveData(PTTState.INACTIVE)

    init {
        channelType.observeForever(this)
    }

    override fun onChanged(t: ChannelType) {
        val t = t ?: return
        Log.i(tag, "changing channel to $t")
        channel = when(t) {
            ChannelType.COLD ->  ColdChannel()
            ChannelType.HOT -> HotChannel()
        }
    }

    fun pttPushed() {
        if (state.value != PTTState.INACTIVE) return
        state.value = PTTState.CONNECTING
        channel.pttToggle()
        Log.i(tag, "PttButton Pushed")
    }

    fun pttStop() {

        Log.i(tag, "PttButton Unpushed")
        if (state.value != PTTState.CONNECTING && state.value != PTTState.BROADCASTING) return
        state.value = PTTState.INACTIVE
        channel.pttToggle()
        Log.i(tag, "PttButton Unpushed Inactive")
    }
}