package io.agora.agorapttdemo.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class PTTState {
    RECEIVING, CONNECTING, BROADCASTING, INACTIVE
}

private val tag = "PTTButtonViewModel"
class PTTButtonViewModel(state: PTTState = PTTState.INACTIVE): ViewModel() {
    var state: MutableLiveData<PTTState> = MutableLiveData(state)

    fun pttPushed() {
        if (state.value != PTTState.INACTIVE) return
        state.value = PTTState.CONNECTING
        Log.i(tag, "PttButton Pushed")
    }

    fun pttStop() {
        Log.i(tag, "PttButton Unpushed")
        if (state.value != PTTState.CONNECTING && state.value != PTTState.BROADCASTING) return
        state.value = PTTState.INACTIVE
        Log.i(tag, "PttButton Unpushed Inactive")

    }
}