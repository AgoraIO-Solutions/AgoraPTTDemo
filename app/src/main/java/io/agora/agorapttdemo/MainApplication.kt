package io.agora.agorapttdemo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.agora.agorapttdemo.utilities.SignalingManager
import io.agora.agorapttdemo.utilities.VoiceManager
import javax.inject.Inject

@HiltAndroidApp
class MainApplication: Application() {
    @Inject lateinit var vm: VoiceManager
    @Inject lateinit var sm: SignalingManager

    override fun onTerminate() {
        super.onTerminate()
        vm.destroy()
        sm.destroy()
    }
}