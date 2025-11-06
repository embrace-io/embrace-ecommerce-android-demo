package io.embrace.shoppingcart.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.embrace.shoppingcart.mock.AuthConfig
import io.embrace.shoppingcart.mock.MockAuthService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MockModule {
    @Provides
    @Singleton
    fun provideAuthConfig(): AuthConfig = AuthConfig()

    @Provides
    @Singleton
    fun provideMockAuthService(config: AuthConfig): MockAuthService = MockAuthService(config)
}
