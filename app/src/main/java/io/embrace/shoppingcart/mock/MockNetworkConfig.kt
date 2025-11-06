package io.embrace.shoppingcart.mock

enum class NetworkScenario { SUCCESS, FAILURE, SLOW, SERVER_ERROR }

data class MockNetworkConfig(
    val defaultDelayMs: Long = 0L,
    val slowDelayMs: Long = 3000L,
    val productsScenario: NetworkScenario = NetworkScenario.SUCCESS,
    val categoriesScenario: NetworkScenario = NetworkScenario.SUCCESS,
    val placeOrderScenario: NetworkScenario = NetworkScenario.SUCCESS,
)
