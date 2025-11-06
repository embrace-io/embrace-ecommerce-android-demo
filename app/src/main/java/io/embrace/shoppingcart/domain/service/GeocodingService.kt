package io.embrace.shoppingcart.domain.service

data class GeoPoint(val latitude: Double, val longitude: Double)

interface GeocodingService {
    suspend fun geocode(address: String): GeoPoint?
}

