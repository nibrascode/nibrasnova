package com.nibras.nova.di

import android.content.Context
import com.nibras.nova.core.security.AdBlockerEngine
import com.nibras.nova.core.security.SchemePolicy
import com.nibras.nova.core.security.SslErrorHandlerPolicy
import com.nibras.nova.engine.IncognitoSessionManager
import com.nibras.nova.engine.WebViewConfigurator
import com.nibras.nova.engine.WebViewPoolManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrowserEngineModule {

    @Provides
    @Singleton
    fun provideWebViewPoolManager(
        @ApplicationContext context: Context,
        configurator: WebViewConfigurator,
        incognitoSessionManager: IncognitoSessionManager,
        schemePolicy: SchemePolicy,
        sslPolicy: SslErrorHandlerPolicy,
        adBlockerEngine: AdBlockerEngine
    ): WebViewPoolManager {
        return WebViewPoolManager(
            appContext = context,
            configurator = configurator,
            incognitoSessionManager = incognitoSessionManager,
            schemePolicy = schemePolicy,
            sslPolicy = sslPolicy,
            adBlockerEngine = adBlockerEngine
        )
    }
}
