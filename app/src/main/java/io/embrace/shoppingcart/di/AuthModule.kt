package io.embrace.shoppingcart.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.embrace.shoppingcart.domain.api.AuthApi
import io.embrace.shoppingcart.mock.MockAuthApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideAuthApi(api: MockAuthApi): AuthApi = api
}

