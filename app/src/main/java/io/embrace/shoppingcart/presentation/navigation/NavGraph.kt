package io.embrace.shoppingcart.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.embrace.shoppingcart.presentation.product.ProductDetailScreen
import io.embrace.shoppingcart.presentation.search.SearchScreen

object Routes {
    const val Search = "search"
    const val ProductDetail = "product_detail/{productId}"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.Search) {
        composable(Routes.Search) { SearchScreen(navController) }
        composable(
                route = Routes.ProductDetail,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                    productId = productId,
                    onBackPressed = { navController.popBackStack() }
            )
        }
    }
}
