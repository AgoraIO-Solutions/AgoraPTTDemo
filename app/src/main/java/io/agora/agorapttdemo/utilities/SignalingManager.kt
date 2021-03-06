package io.agora.agorapttdemo.utilities

import android.net.wifi.aware.SubscribeConfig
import android.util.Log
import com.google.gson.Gson
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects.channel.PNChannelMetadata
import com.pubnub.api.models.consumer.objects.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.objects.PNObjectEventResult
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.subjects.AsyncSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.ReplaySubject
import io.reactivex.rxjava3.subjects.Subject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

@Module
@InstallIn(SingletonComponent::class)
class SignalingManagerModule {
    fun sm(config: Config): SignalingManager {
        return SignalingManager(config = config)
    }
}


enum class Signal {
    RB, // REQUEST_BROADCAST
    RR, // REQUEST_BROADCAST_REFRESH
    AB, // ACKNOWLEDGE BROADCAST
    EB // END BROADCAST
}

data class Message(val s: Signal, val i: UUID) {
    companion object {
        fun requestBroadcast() : Message {
            return Message(Signal.RB, UUID.randomUUID())
        }
    }
}

fun Message.refreshBroadcast(): Message {
    return Message(Signal.RR, i)
}

fun Message.endBroadcast() : Message {
    return Message(Signal.EB, i)
}

fun Message.ackBroadcast() : Message {
    return Message(Signal.AB, i)
}

private val tag = "SignalingManager"

class SignalingManager @Inject constructor(private val config: Config): SubscribeCallback() {
    private val gson = Gson()
    private val pubnub: PubNub
    private val channels = listOf(config.channel)
    private var channelMetadata: PNChannelMetadata? = null
    val presenceSubject: ReplaySubject<PNPresenceEventResult> =  ReplaySubject.createWithSize(1)
    val messagingSubject: PublishSubject<Message> = PublishSubject.create()
    val channelObjectMetaDataSubject: ReplaySubject<PNChannelMetadata> = ReplaySubject.createWithSize(1)

    init {
        val pnConfiguration = PNConfiguration().apply {
            this.subscribeKey = config.pnSub
            this.publishKey = config.pnPub
            this.uuid = config.uuid.toString()
            this.presenceTimeout = 20 // This is unideal for detecting presence in a quorom
        }
        pubnub = PubNub(pnConfiguration)
        pubnub.addListener(this)

        pubnub.subscribe(channels = channels, withPresence = true)
        pubnub.hereNow(
            channels = channels,
            includeState = true,
            includeUUIDs = true
        ).async { result, status ->
            if (status.error) {
                // handle error
                status.exception?.printStackTrace()
                return@async
            }
            result!!.channels.values.forEach { channelData ->
                Log.i(tag,"---")
                Log.i(tag,"Channel: ${channelData.channelName}")
                Log.i(tag,"Occupancy: ${channelData.occupancy}")
                Log.i(tag,"Occupants:")

                channelData.occupants.forEach { o ->
                    Log.i(tag,"UUID: ${o.uuid}, state: ${o.state}")
                }
            }
        }
        refreshChannelMetaData()
    }

    override fun objects(pubnub: PubNub, objectEvent: PNObjectEventResult) {
        Log.i(tag, "reeived object result")
        refreshChannelMetaData()
    }
    private fun refreshChannelMetaData() {
        pubnub.getChannelMetadata(config.channel, includeCustom = true).async { result, status ->
            if (status.error) {
                Log.i(tag, "objec Error $status")
            }
            val data = result?.data ?: return@async
            channelObjectMetaDataSubject.onNext(data)
            this.channelMetadata = data
        }
    }

    override fun status(pubnub: PubNub, pnStatus: PNStatus) {
        Log.i(tag, "status received $pnStatus")
    }

    override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
        this.presenceSubject.onNext(pnPresenceEventResult)
    }

    override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {
        if (pnSignalResult.publisher == config.uuid.toString()) return
        Log.i(tag, "signal received $pnSignalResult")
        try {
            val parsed = gson.fromJson(pnSignalResult.message.asString, Message::class.java)
            Log.i(tag, "parsed signal too $parsed")
            messagingSubject.onNext(parsed)
        } catch (throwable: Throwable) {
            Log.e(tag, "Error parsing signal")
        }
    }

    fun sendSignal(message: Message) {
        pubnub.signal(channel = config.channel, message = gson.toJson(message)).async { result, status ->
            if (!status.error) {
                Log.i(tag,"Timetoken: ${result?.timetoken}")
            } else {
                // handle error
                status.exception?.printStackTrace()
            }
        }
    }

    fun destroy() {
        pubnub.unsubscribe(channels)
    }

    fun addChannelMetaData(map: Map<String, Any>) {
        val mutableMap = (channelMetadata?.custom as? Map<String, Any> ?: emptyMap()).toMutableMap()
        map.forEach { (key, value) ->
            mutableMap[key] = value
        }
        pubnub.setChannelMetadata(channel = config.channel, custom = mutableMap.toMap()).async { result, status ->
            if (status.error) {
                Log.e(tag, "Error setting meta data")
            } else {
                Log.i(tag, "Set meta data")
            }
        }
    }

    fun removeChannelMetaData(key: String) {
        val mutableMap = (channelMetadata?.custom as? Map<String, Any> ?: emptyMap()).toMutableMap()
        mutableMap.remove(key)
        pubnub.setChannelMetadata(channel = config.channel, custom = mutableMap.toMap()).async { result, status ->
            if (status.error) {
                Log.e(tag, "Error setting meta data")
            } else {
                Log.i(tag, "Set meta data")
            }
        }
    }
}