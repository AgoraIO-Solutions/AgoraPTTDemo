package io.agora.agorapttdemo.utilities

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pubnub.api.models.consumer.objects.channel.PNChannelMetadata
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.*
import org.joda.time.DateTime
import javax.inject.Inject
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class ChannelLockModule {
    @Provides
    @Singleton
    fun channelLock(signalingManager: SignalingManager, voiceManager: VoiceManager): ChannelLock {
        return ChannelLock(signalingManager = signalingManager, voiceManager = voiceManager)
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

// NOTE: Lock collision is not handled in this demo, it should be trivial to understand how it can be use ts and whichever is sooner wins
class ChannelLock @Inject constructor(val signalingManager: SignalingManager, val voiceManager: VoiceManager) {
    enum class State {
        LOCKED, LOCKING, UNLOCKED, LOCKED_BY_OTHER
    }
    val stateData: LiveData<State>
        get() = _stateData

    private val defaultDispatcher = CoroutineScope(Dispatchers.Default)
    private val mainScope = MainScope()
    private var _stateData: MutableLiveData<State> = MutableLiveData(State.UNLOCKED)
    private var metaData: PNChannelMetadata? = null
    private val metaDataDisposable: Disposable = signalingManager.channelObjectMetaDataSubject.subscribe {
        this.metaData = it
    }
    private val messagingDisposable: Disposable = signalingManager.messagingSubject.subscribe(this::handleMessage)
    private var acquireWaiter: AcquireWaiter? = null
    private var refresherJob: Job? = null
    private var refreshMonitorJob: Job? = null
    private var broadcastRequest = Message.requestBroadcast()
    private var lastRefreshRequest = DateTime.now().minusDays(1)

    private fun isLocked(): Boolean {
        if (lastRefreshRequest.isAfter(DateTime.now().minusSeconds(2))) {
            Log.i(tag, "is locked by refresh request")
            return true
        }

        val metadataObj = (metaData?.custom as? Map<*, *>)?.get(lockKey) as? Long ?: return true
        Log.i(tag, "checking lock $metadataObj")
        return  !isExpired(metadataObj)
    }

    private fun safeSetState(state: State) {
        mainScope.launch(Dispatchers.Main) {
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
        defaultDispatcher.launch {
            refresherJob?.cancelAndJoin()
            refresherJob = null
            safeSetState(State.UNLOCKED)
        }
    }

    private fun refreshChannelLock() {
        signalingManager.addChannelMetaData(mapOf(lockKey to newExpiration()))
    }

    private fun handleMessage(message: Message) {
        when (message.s) {
            Signal.AB -> { /* noop */ }
            Signal.RB -> {
                if (_stateData.value == State.LOCKED_BY_OTHER) return
                refreshMonitorJob = startRefreshMonitorJob(message)
                safeSetState(State.LOCKED_BY_OTHER)
                voiceManager.listen { signalingManager.sendSignal(message = message.ackBroadcast()) }
            }
            Signal.RR -> {
                lastRefreshRequest = DateTime.now()
                if (_stateData.value == State.LOCKED_BY_OTHER) return
                Log.i(tag, "handle New Broadcast")
                if (refreshMonitorJob != null || refreshMonitorJob?.isCompleted == true) return
                refreshMonitorJob = startRefreshMonitorJob(message)
                safeSetState(State.LOCKED_BY_OTHER)
            }
            Signal.EB -> {
                Log.i(tag, "handle End Broadcast")
                defaultDispatcher.launch {
                    refreshMonitorJob?.cancelAndJoin()
                    safeSetState(State.UNLOCKED)
                    lastRefreshRequest = DateTime.now().minusMinutes(1)
                    Log.i(tag,"stopped refresh monitoring")
                }
            }
        }
    }

    protected fun finalize() {
        releaseLock()
        metaDataDisposable.dispose()
        messagingDisposable.dispose()
    }

    private fun lockAcquired() {
        acquireWaiter?.stop()
        acquireWaiter = null

        safeSetState(State.LOCKED)
        refresherJob = startRefreshLockJob()
    }

    private suspend fun releaseTimedOutLock(message: Message) {
        Log.i("$tag:RefreshMonitor", "lock timed out deactivating refresher")
        signalingManager.removeChannelMetaData(lockKey)
        signalingManager.sendSignal(message.endBroadcast())
        refreshMonitorJob?.cancelAndJoin()
        refreshMonitorJob = null
        safeSetState(State.UNLOCKED)
    }

    private inner class AcquireWaiter {
        private val innerTag = "$tag:AcquireWaiter"
        private val countdownJob: Job = defaultDispatcher.launch {
            var i = 0
            while(i < 15) {
                delay(100)
                i++
                Log.i(innerTag, "milliseconds until timeout ${100 * i}")
            }
            this@ChannelLock.lockAcquired()
            this@ChannelLock.acquireWaiter = null
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
            defaultDispatcher.launch {
                countdownJob.cancelAndJoin()
                lockAcquired()
            }
        }

        fun stop() {
            presenceDisposable.dispose()
            messageDisposable.dispose()
            defaultDispatcher.launch {
                countdownJob.cancelAndJoin()
            }
        }
    }

    private fun startRefreshLockJob(): Job {
        return defaultDispatcher.launch {
            while (isActive) {
                delay(1 * 1000)
                Log.i(tag, "refreshing lock")
                signalingManager.sendSignal(broadcastRequest.refreshBroadcast())
                refreshChannelLock()
            }
        }
    }

    private fun startRefreshMonitorJob(broadcastMessage: Message): Job {
        return defaultDispatcher.launch {
            while (isActive) {
                Log.i(tag, "refresh monitor is checking for lock or last activity")
                delay(1 * 1000)
                if (!isLocked()) {
                    releaseTimedOutLock(message = broadcastMessage)
                }
            }
        }
    }
}