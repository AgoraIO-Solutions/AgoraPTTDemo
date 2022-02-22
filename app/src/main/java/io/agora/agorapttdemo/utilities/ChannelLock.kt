package io.agora.agorapttdemo.utilities

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pubnub.api.models.consumer.objects.channel.PNChannelMetadata
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.*
import org.joda.time.DateTime
import java.lang.Runnable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sign


@Module
@InstallIn(SingletonComponent::class)
class ChannelLockModule {
    @Provides
    @Singleton
    fun channelLock(signalingManager: SignalingManager): ChannelLock {
        return ChannelLock(signalingManager = signalingManager)
    }
}

private fun newExpiration(): Long {
    return DateTime.now().plusSeconds(3).millis
}

private fun isExpired(lockTime: Long): Boolean {
    val nowInMillis = DateTime.now().millis
    return lockTime <= nowInMillis
}

private const val tag = "ChannelLock"
private const val lockKey = "-lock-ch"
private const val updateInterval = 500L

// NOTE: Lock collision is not handled in this demo, it should be trivial to understand how it can be use ts and whichever is sooner wins
class ChannelLock @Inject constructor(val signalingManager: SignalingManager) {
    enum class State {
        LOCKED, LOCKING, UNLOCKED, LOCKED_BY_OTHER
    }
    val stateData: LiveData<State>
        get() = _stateData

    private var _stateData: MutableLiveData<State> = MutableLiveData(State.UNLOCKED)
    private var metaData: PNChannelMetadata? = null
    private val metaDataDisposable: Disposable = signalingManager.channelObjectMetaDataSubject.subscribe {
        this.metaData = it
    }
    private val messagingDisposable: Disposable = signalingManager.messagingSubject.subscribe(this::handleMessage)
    private var acquireWaiter: AcquireWaiter? = null
    private var refresher: Refresher? = null
    private var refreshMonitor: RefreshMonitor? = null
    private var broadcastRequest = Message.requestBroadcast()

    private fun isLocked(): Boolean {
        val metadataObj = (metaData?.custom as? Map<*, *>)?.get(lockKey) as? Long ?: return true
        Log.i(tag, "checking lock $metadataObj")
        return  !isExpired(metadataObj)
    }

    private val aScope = MainScope()
    private fun safeSetState(state: State) {
        aScope.launch(Dispatchers.Main) {
            _stateData.value = state
        }
    }

    // Tries to lock the channel,
    fun acquireLock() {
        if (_stateData.value != State.UNLOCKED) return

        if (!isLocked()) {
            Log.i(tag, "The channel is locked still")
            return
        }
        // Lock Channel Meta data
        safeSetState(State.LOCKING)
        refreshChannelLock()
        broadcastRequest = Message.requestBroadcast()
        acquireWaiter = AcquireWaiter()
    }
    fun releaseLock() {
        signalingManager.removeChannelMetaData(lockKey)
        signalingManager.sendSignal(broadcastRequest.endBroadcast())
        refresher?.stop()
        refresher = null
        safeSetState(State.UNLOCKED)
    }

    private fun refreshChannelLock() {
        signalingManager.addChannelMetaData(mapOf(lockKey to newExpiration()))
    }

    private fun handleMessage(message: Message) {
        when (message.s) {
            Signal.AB -> { /* noop */ }
            Signal.RB -> {
                if (_stateData.value == State.LOCKED_BY_OTHER) return
                refreshMonitor = RefreshMonitor(message)
                safeSetState(State.LOCKED_BY_OTHER)
            }
            Signal.RR -> {
                if (_stateData.value == State.LOCKED_BY_OTHER) return
                Log.i(tag, "handle New Broadcast")
                if (refreshMonitor != null) return
                refreshMonitor = RefreshMonitor(message)
                safeSetState(State.LOCKED_BY_OTHER)
            }
            Signal.EB -> {
                Log.i(tag, "handle End Broadcast")
                refresher?.stop()
                refresher = null
                safeSetState(State.UNLOCKED)
            }
        }
    }

    protected fun finalize() {
        releaseLock()
        metaDataDisposable.dispose()
        messagingDisposable.dispose()
    }

    private fun lockAcquired() {
        acquireWaiter = null
        safeSetState(State.LOCKED)
        refresher = Refresher()
    }

    private fun releaseTimedOutLock(message: Message) {
        Log.i("$tag:RefreshMonitor", "lock timed out deactivationg refresher")
        signalingManager.removeChannelMetaData(lockKey)
        signalingManager.sendSignal(message.endBroadcast())
        refreshMonitor?.stop()
        refreshMonitor = null
        safeSetState(State.UNLOCKED)
    }

    private inner class AcquireWaiter {
        private val innerTag = "$tag:AcquireWaiter"
        private val timer: CountDownTimer = object : CountDownTimer(500, 100) {
            override fun onTick(p0: Long) {
                Log.i(innerTag, "seconds until timeout $p0")
            }

            override fun onFinish() {
                Log.i(innerTag, "timeout occured, grabbing locky")
                lockAcquired()
            }
        }.apply {
            this.start()
        }

        init {
            signalingManager.sendSignal(broadcastRequest)
        }
        private var confirmations = 0
            set(value)  {
                field = value
                checkConfirmations()
            }
        private var presence = 0
            set(value)  {
                field = value
                checkConfirmations()
            }
        private var presenceDisposable: Disposable = signalingManager.presenceSubject.subscribe {
            this.presence = it.occupancy ?: 0
        }

        private var messageDisposable: Disposable = signalingManager.messagingSubject.subscribe(this::handleMessage)

        private fun handleMessage(message: Message) {
            if (message.s != Signal.AB) return
            confirmations += 1
        }

        private fun checkConfirmations() {
            if (confirmations < presence - 1) return
            lockAcquired()
        }

        protected fun finalize() {
            presenceDisposable.dispose()
            messageDisposable.dispose()
            timer.cancel()
        }
    }

    private inner class Refresher {
        private val periodicTask = Runnable {
            Log.i("$tag:Refresher", "refreshing lock")
            signalingManager.sendSignal(broadcastRequest.refreshBroadcast())
            refreshChannelLock()
        }

        private val executorService = Executors.newScheduledThreadPool(1)
            .apply {
                this.scheduleAtFixedRate(periodicTask, 1L, 1L, TimeUnit.SECONDS)
        }

        init {
            Log.i("$tag:Refresher", "Start")
        }

        fun stop() {
            Log.i("$tag:Refresher", "Stop")
            executorService.shutdown()
        }
    }

    private inner class RefreshMonitor(broadcastMessage: Message) {
        private val periodicTask = Runnable {
            Log.i("$tag:RefreshMonitor", "deactivating refresh monitor")
            if (isLocked()) return@Runnable // locked do nothing
            releaseTimedOutLock(broadcastMessage)
        }


        private val executorService = Executors.newScheduledThreadPool(1)
            .apply {
                val thing = this.scheduleAtFixedRate(periodicTask, 1L,1L , TimeUnit.SECONDS)
                thing.cancel(true)
            }
        init {
            Log.i("$tag:RefreshMonitor", "Start")
        }

        fun stop() {
            Log.i("$tag:RefreshMonitor", "Stop")
            executorService.shutdown()
        }
    }
}