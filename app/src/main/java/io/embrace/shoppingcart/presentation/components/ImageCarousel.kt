package io.embrace.shoppingcart.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCarousel(imageUrls: List<String>, modifier: Modifier = Modifier) {
    if (imageUrls.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = modifier) {
        // Carrusel principal
        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                        model =
                                ImageRequest.Builder(context)
                                        .data(imageUrls[page])
                                        .crossfade(true)
                                        .build(),
                        contentDescription = "Imagen ${page + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                )
            }

            // Indicadores de página
            if (imageUrls.size > 1) {
                Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    imageUrls.forEachIndexed { index, _ ->
                        Box(
                                modifier =
                                        Modifier.size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        if (pagerState.currentPage == index)
                                                                MaterialTheme.colorScheme.primary
                                                        else
                                                                MaterialTheme.colorScheme.onSurface
                                                                        .copy(alpha = 0.3f)
                                                )
                        )
                    }
                }
            }
        }

        // Miniaturas
        if (imageUrls.size > 1) {
            LazyRow(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                itemsIndexed(imageUrls) { index, imageUrl ->
                    AsyncImage(
                            model =
                                    ImageRequest.Builder(context)
                                            .data(imageUrl)
                                            .crossfade(true)
                                            .build(),
                            contentDescription = null,
                            modifier =
                                    Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                            contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
