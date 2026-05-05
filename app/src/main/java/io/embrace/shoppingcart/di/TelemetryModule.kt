package io.embrace.shoppingcart.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.embrace.shoppingcart.telemetry.EmbraceTelemetryService
import io.embrace.shoppingcart.telemetry.TelemetryService
import javax.inject.Singleton

/**
 * Binds [TelemetryService] → [EmbraceTelemetryService] for production builds.
 *
 * Test builds can replace this module via `@TestInstallIn` or `@UninstallModules`
 * and provide [io.embrace.shoppingcart.telemetry.NoOpTelemetryService] instead.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TelemetryModule {

    @Binds
    @Singleton
    abstract fun bindTelemetryService(impl: EmbraceTelemetryService): TelemetryService
}
