package io.embrace.shoppingcart.ui.product

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.embrace.shoppingcart.presentation.product.ProductDetailScreen
import io.embrace.shoppingcart.ui.theme.EmbraceShoppingCartTheme

@AndroidEntryPoint
class ProductDetailActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_PRODUCT_ID = "extra_product_id"

        fun createIntent(context: Context, productId: String): Intent {
            return Intent(context, ProductDetailActivity::class.java).apply {
                putExtra(EXTRA_PRODUCT_ID, productId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val productId = intent.getStringExtra(EXTRA_PRODUCT_ID) ?: ""

        setContent {
            EmbraceShoppingCartTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) { 
                    ProductDetailScreen(productId = productId, onBackPressed = { finish() }) 
                }
            }
        }
    }
}
