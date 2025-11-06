package io.embrace.shoppingcart.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.embrace.shoppingcart.data.local.AppDatabase
import io.embrace.shoppingcart.data.local.ProductDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "shopping.db")
                    .fallbackToDestructiveMigration(true)
                    .build()

    @Provides @Singleton fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides @Singleton fun provideUserDao(db: AppDatabase) = db.userDao()
    @Provides @Singleton fun provideAddressDao(db: AppDatabase) = db.addressDao()
    @Provides @Singleton fun providePaymentMethodDao(db: AppDatabase) = db.paymentMethodDao()
    @Provides @Singleton fun provideCartItemDao(db: AppDatabase) = db.cartItemDao()
    @Provides @Singleton fun provideOrderDao(db: AppDatabase) = db.orderDao()
}
