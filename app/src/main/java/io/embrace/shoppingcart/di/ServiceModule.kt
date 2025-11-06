package io.embrace.shoppingcart.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.embrace.shoppingcart.domain.service.GeocodingService
import io.embrace.shoppingcart.mock.MockGeocodingService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    @Provides
    @Singleton
    fun provideGeocodingService(impl: MockGeocodingService): GeocodingService = impl
}

