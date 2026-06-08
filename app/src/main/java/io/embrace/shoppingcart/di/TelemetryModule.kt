package io.embrace.shoppingcart.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.embrace.shoppingcart.telemetry.EmbraceTelemetryService
import io.embrace.shoppingcart.telemetry.TelemetryService
import javax.inject.Singleton

/**
 * Provides [TelemetryService] → [EmbraceTelemetryService.instance] for
 * production builds.
 *
 * Deliberately the SAME object as the companion accessor: the wrapper now
 * holds state (consent, TelemetryConfig, rate-limiter windows), so Hilt
 * call sites and direct `EmbraceTelemetryService.instance` accessors
 * (Application.onCreate, top-level Composables) must share it.
 *
 * Test builds can replace this module via `@TestInstallIn` or `@UninstallModules`
 * and provide [io.embrace.shoppingcart.telemetry.NoOpTelemetryService] instead.
 */
@Module
@InstallIn(SingletonComponent::class)
object TelemetryModule {

    @Provides
    @Singleton
    fun provideTelemetryService(): TelemetryService = EmbraceTelemetryService.instance
}
