package io.agora.agorapttdemo.utilities

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.agora.agorapttdemo.R
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ConnectingSound

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SendingSound

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReceivingSound


@Module
@InstallIn(SingletonComponent::class)
class AlerterModule {
    @SendingSound
    @Provides
    @Singleton
    fun sendAlerter(@ApplicationContext context: Context): Alerter {
        return Alerter(Alerter.Type.Send, context)
    }

    @ConnectingSound
    @Provides
    @Singleton
    fun connectAlerter(@ApplicationContext context: Context): Alerter {
        return Alerter(Alerter.Type.Connect, context)
    }

    @ReceivingSound
    @Provides
    @Singleton
    fun receiveAlerter(@ApplicationContext context: Context): Alerter {
        return Alerter(Alerter.Type.Receive, context)
    }
}

class Alerter @Inject constructor(type: Type, context: Context) {
    private val mediaPlayer = MediaPlayer.create(context, when(type) {
        Type.Connect -> R.raw.connecting_sound
        Type.Receive -> R.raw.receive_alert
        Type.Send -> R.raw.send_alert
    })

    enum class Type {
        Send, Receive, Connect
    }

    fun play(onDone: () -> Unit) {
        mediaPlayer.setOnCompletionListener {
            onDone()
        }
        mediaPlayer.start()
    }

    fun stop() {
        mediaPlayer.stop()
        mediaPlayer.reset()
        mediaPlayer.prepare()
    }
}