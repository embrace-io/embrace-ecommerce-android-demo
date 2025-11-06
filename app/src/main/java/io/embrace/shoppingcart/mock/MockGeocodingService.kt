package io.embrace.shoppingcart.mock

import io.embrace.shoppingcart.domain.service.GeoPoint
import io.embrace.shoppingcart.domain.service.GeocodingService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MockGeocodingService @Inject constructor() : GeocodingService {
    override suspend fun geocode(address: String): GeoPoint? {
        // Deterministic mock lat/lng based on hash
        val seed = address.hashCode().toLong()
        val rnd = Random(seed)
        val lat = -60 + rnd.nextDouble() * 120 // [-60, 60]
        val lon = -170 + rnd.nextDouble() * 340 // [-170, 170]
        return GeoPoint(lat, lon)
    }
}

