package io.agora.agorapttdemo.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChannelConfigurationViewModel: ViewModel() {
    enum class ChannelType {
        COLD, HOT
    }

    val channelType: MutableLiveData<ChannelType> = MutableLiveData(ChannelType.HOT)
    val alertsOn: MutableLiveData<Boolean> = MutableLiveData(true)
}