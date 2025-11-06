package io.embrace.shoppingcart.mock

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.embrace.android.embracesdk.Embrace
import io.embrace.android.embracesdk.network.EmbraceNetworkRequest
import io.embrace.android.embracesdk.network.http.HttpMethod
import io.embrace.shoppingcart.network.ApiService
import io.embrace.shoppingcart.network.ProductDto
import io.embrace.shoppingcart.network.CategoryDto
import io.embrace.shoppingcart.network.order.OrderRequest
import io.embrace.shoppingcart.network.order.OrderResponse
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject

class MockApiService @Inject constructor(
    private val context: Context,
    private val moshi: Moshi,
    private val config: MockNetworkConfig
) : ApiService {

    private val productAdapter = moshi.adapter<List<ProductDto>>(
        Types.newParameterizedType(List::class.java, ProductDto::class.java)
    )

    private val categoryAdapter = moshi.adapter<List<CategoryDto>>(
        Types.newParameterizedType(List::class.java, CategoryDto::class.java)
    )

    private fun currentConfig(): MockNetworkConfig = MockNetworkConfigOverrides.override ?: config

    override suspend fun getProducts(): List<ProductDto> {
        val url = "https://api.ecommerce.com/products"
        val start = System.currentTimeMillis()
        val cfg = currentConfig()
        return when (cfg.productsScenario) {
            NetworkScenario.SUCCESS -> {
                delay(cfg.defaultDelayMs)
                val result = loadProducts()
                val end = System.currentTimeMillis()
                recordCompletedNetworkRequest(url, HttpMethod.GET, start, end, 200)
                result
            }
            NetworkScenario.FAILURE -> {
                delay(cfg.defaultDelayMs)
                val end = System.currentTimeMillis()
                recordIncompletedNetworkRequest(url, HttpMethod.GET, start, end, "Simulated network failure")
                throw IOException("Simulated network failure")
            }
            NetworkScenario.SLOW -> {
                delay(cfg.slowDelayMs)
                val result = loadProducts()
                val end = System.currentTimeMillis()
                recordCompletedNetworkRequest(url, HttpMethod.GET, start, end, 200)
                result
            }
            NetworkScenario.SERVER_ERROR -> {
                delay(cfg.defaultDelayMs)
                val end = System.currentTimeMillis()
                recordCompletedNetworkRequest(url, HttpMethod.GET, start, end, 500)
                throw IOException("Simulated server error (500)")
            }
        }
    }

    override suspend fun getCategories(): List<CategoryDto> {
        val url = "https://api.ecommerce.com/categories"
        val start = System.currentTimeMillis()
        val cfg = currentConfig()
        return when (cfg.categoriesScenario) {
            NetworkScenario.SUCCESS -> {
                delay(cfg.defaultDelayMs)
                val result = loadCategories()
                val end = System.currentTimeMillis()
                recordCompletedNetworkRequest(url, HttpMethod.GET, start, end, 200)
                result
            }
            NetworkScenario.FAILURE -> {
                delay(cfg.defaultDelayMs)
                val end = System.currentTimeMillis()
                recordIncompletedNetworkRequest(url, HttpMethod.GET, start, end, "Simulated network failure")
                throw IOException("Simulated network failure")
            }
            NetworkScenario.SLOW -> {
                delay(cfg.slowDelayMs)
                val result = loadCategories()
                val end = System.currentTimeMillis()
                recordCompletedNetworkRequest(url, HttpMethod.GET, start, end, 200)
                result
            }
            NetworkScenario.SERVER_ERROR -> {
                delay(cfg.defaultDelayMs)
                val end = System.currentTimeMillis()
                recordCompletedNetworkRequest(url, HttpMethod.GET, start, end, 500)
                throw IOException("Simulated server error (500)")
            }
        }
    }

    override suspend fun placeOrder(request: OrderRequest): OrderResponse {
        val url = "https://api.ecommerce.com/orders"
        val start = System.currentTimeMillis()
        val cfg = currentConfig()
        return when (cfg.placeOrderScenario) {
            NetworkScenario.SUCCESS -> {
                delay(cfg.defaultDelayMs)
                val response = OrderResponse(orderId = "ord-${System.currentTimeMillis()}")
                val end = System.currentTimeMillis()
                recordCompletedNetworkRequest(url, HttpMethod.POST, start, end, 200)
                response
            }
            NetworkScenario.FAILURE -> {
                delay(cfg.defaultDelayMs)
                val end = System.currentTimeMillis()
                recordIncompletedNetworkRequest(url, HttpMethod.POST, start, end, "Simulated order placement failure")
                throw IOException("Connection error failure")
            }
            NetworkScenario.SLOW -> {
                delay(cfg.slowDelayMs)
                val response = OrderResponse(orderId = "ord-${System.currentTimeMillis()}")
                val end = System.currentTimeMillis()
                recordCompletedNetworkRequest(url, HttpMethod.POST, start, end, 200)
                response
            }
            NetworkScenario.SERVER_ERROR -> {
                delay(cfg.defaultDelayMs)
                val end = System.currentTimeMillis()
                recordCompletedNetworkRequest(url, HttpMethod.POST, start, end, 500)
                throw IOException("Server error (500)")
            }
        }
    }

    private fun loadProducts(): List<ProductDto> {
        val json = context.assets.open("products.json").bufferedReader().use { it.readText() }
        return productAdapter.fromJson(json) ?: emptyList()
    }

    private fun loadCategories(): List<CategoryDto> {
        val json = context.assets.open("categories.json").bufferedReader().use { it.readText() }
        return categoryAdapter.fromJson(json) ?: emptyList()
    }

    private fun recordCompletedNetworkRequest(
        url: String,
        httpMethod: HttpMethod,
        startTime: Long,
        endTime: Long,
        statusCode: Int
        ) {
        Embrace.getInstance().recordNetworkRequest(
            EmbraceNetworkRequest.fromCompletedRequest(
                url = url,
                httpMethod = httpMethod,
                startTime = startTime,
                endTime = endTime,
                bytesSent = 0,
                bytesReceived = 0,
                statusCode = statusCode
            )
        )
    }

    private fun recordIncompletedNetworkRequest(
        url: String,
        httpMethod: HttpMethod,
        startTime: Long,
        endTime: Long,
        errorMessage: String
    ) {
        Embrace.getInstance().recordNetworkRequest(
            EmbraceNetworkRequest.fromIncompleteRequest(
                url = url,
                httpMethod = httpMethod,
                startTime = startTime,
                endTime = endTime,
                errorType = "Connection Error",
                errorMessage = errorMessage
            )
        )
    }
}
