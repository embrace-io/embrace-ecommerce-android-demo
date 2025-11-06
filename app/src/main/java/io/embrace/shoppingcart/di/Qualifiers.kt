package io.embrace.shoppingcart.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RealApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MockApi


