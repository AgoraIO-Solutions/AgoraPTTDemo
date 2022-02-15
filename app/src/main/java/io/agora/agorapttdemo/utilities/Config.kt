package io.agora.agorapttdemo.utilities

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.agora.agorapttdemo.R
import java.util.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ConfigModule {
    @Provides
    @Singleton
    fun config(@ApplicationContext context: Context): Config {
        return Config(context)
    }
}

class Config(context: Context) {
    val appId = context.getString(R.string.agora_app_id)
    val channel = context.getString(R.string.test_channel)
    val pnPub = context.getString(R.string.pubnub_publish)
    val pnSub = context.getString(R.string.pubnub_subscribe)
    val tempToken: String? = null
    val uuid = UUID.randomUUID()
}