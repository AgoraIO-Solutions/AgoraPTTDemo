package io.agora.agorapttdemo.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class PTTState {
    RECEIVING, CONNECTING, BROADCASTING, INACTIVE
}

class PTTButtonViewModel(state: PTTState): ViewModel() {
    var state: MutableLiveData<PTTState> = MutableLiveData(state)
}