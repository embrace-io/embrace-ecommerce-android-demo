# Detalle de Producto - Nueva Actividad con Compose

## Descripción

Se ha implementado una nueva actividad de detalle de producto usando Jetpack Compose con las siguientes características:

### 🖼️ Carrusel de Imágenes con Coil
- **ImageCarousel**: Componente reutilizable que muestra múltiples imágenes del producto
- **Navegación por gestos**: Deslizar para cambiar entre imágenes
- **Indicadores visuales**: Puntos que muestran la imagen actual
- **Miniaturas**: Vista previa de todas las imágenes con navegación directa
- **Carga optimizada**: Uso de Coil con crossfade para transiciones suaves

### 🎨 Selección de Variantes
- **ProductVariantsSection**: Componente para seleccionar tamaño y color
- **FilterChip**: Chips interactivos para cada opción de variante
- **Estado persistente**: Las selecciones se mantienen durante la sesión
- **Validación**: Solo muestra variantes disponibles

### 🛒 Funcionalidad del Carrito
- **AddToCartSection**: Contador de cantidad y botón de agregar al carrito
- **Contador interactivo**: Botones +/- para ajustar cantidad
- **Validación de stock**: Botón deshabilitado si el producto está agotado
- **Feedback visual**: Mensajes de confirmación al agregar al carrito

### 📤 Funcionalidad de Compartir
- **Botón de compartir**: En la barra superior de la actividad
- **Mensajes informativos**: Snackbar con confirmación de compartir
- **Integración nativa**: Listo para implementar compartir real

## Componentes Creados

### 1. ImageCarousel
```kotlin
@Composable
fun ImageCarousel(
    imageUrls: List<String>,
    modifier: Modifier = Modifier
)
```

### 2. ProductVariantsSection
```kotlin
@Composable
fun ProductVariantsSection(
    sizes: List<String>,
    colors: List<String>,
    selectedVariant: ProductVariant,
    onSizeSelected: (String) -> Unit,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

### 3. AddToCartSection
```kotlin
@Composable
fun AddToCartSection(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onAddToCart: () -> Unit,
    isProductAvailable: Boolean,
    modifier: Modifier = Modifier
)
```

### 4. MessageSnackbar
```kotlin
@Composable
fun MessageSnackbar(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```

## Arquitectura

### ViewModel (ProductDetailViewModel)
- **Estado centralizado**: ProductDetailUiState maneja todo el estado
- **Funciones de negocio**: 
  - `loadProduct(productId)`: Carga el producto por ID
  - `selectSize(size)`: Selecciona tamaño
  - `selectColor(color)`: Selecciona color
  - `updateQuantity(quantity)`: Actualiza cantidad
  - `addToCart()`: Agrega al carrito
  - `shareProduct()`: Comparte el producto

### Modelos de Datos
```kotlin
data class Product(
    val id: String,
    val name: String,
    val priceCents: Int,
    val description: String,
    val imageUrls: List<String>, // Múltiples imágenes
    val variants: ProductVariants // Variantes de producto
)

data class ProductVariants(
    val sizes: List<String>,
    val colors: List<String>
)

data class ProductVariant(
    val size: String? = null,
    val color: String? = null
)
```

## Navegación

### Rutas
```kotlin
object Routes {
    const val Search = "search"
    const val ProductDetail = "product_detail/{productId}"
}
```

### Navegación desde SearchScreen
```kotlin
navController.navigate(Routes.ProductDetail.replace("{productId}", product.id))
```

## Datos de Ejemplo

Los productos en `products.json` han sido actualizados para incluir:
- **Múltiples imágenes**: Array de URLs de imágenes
- **Variantes**: Tamaños y colores disponibles
- **Descripciones extendidas**: Texto más detallado

### Ejemplo de producto actualizado:
```json
{
  "id": "1",
  "name": "Camiseta de Algodón Premium",
  "priceCents": 1999,
  "description": "Camiseta suave de algodón 100% con corte moderno y cómodo...",
  "imageUrls": [
    "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400&h=400&fit=crop",
    "https://images.unsplash.com/photo-1503341504253-dff4815485f1?w=400&h=400&fit=crop",
    "https://images.unsplash.com/photo-1581655353564-dff123a1eb820?w=400&h=400&fit=crop"
  ],
  "variants": {
    "sizes": ["XS", "S", "M", "L", "XL", "XXL"],
    "colors": ["Blanco", "Negro", "Azul", "Gris"]
  }
}
```

## Características Técnicas

### Dependencias Utilizadas
- **Coil**: Para carga de imágenes con crossfade
- **Navigation Compose**: Para navegación entre pantallas
- **Material 3**: Para componentes de UI modernos
- **Hilt**: Para inyección de dependencias
- **Coroutines**: Para operaciones asíncronas

### Patrones de Diseño
- **MVVM**: ViewModel separado de la UI
- **Repository Pattern**: Para acceso a datos
- **Composable Pattern**: Componentes reutilizables
- **State Hoisting**: Estado elevado para mejor testabilidad

## Próximos Pasos

1. **Implementar carrito real**: Conectar con base de datos local
2. **Funcionalidad de compartir**: Integrar con Intent de Android
3. **Favoritos**: Agregar funcionalidad de productos favoritos
4. **Reviews**: Sistema de reseñas y calificaciones
5. **Recomendaciones**: Productos relacionados
6. **Animaciones**: Transiciones más fluidas entre pantallas

## Uso

Para navegar al detalle de un producto:

```kotlin
// Desde cualquier pantalla con NavController
navController.navigate(Routes.ProductDetail.replace("{productId}", "1"))

// O usando la actividad directamente
val intent = ProductDetailActivity.createIntent(context, "1")
startActivity(intent)
```

La nueva actividad proporciona una experiencia de usuario moderna y completa para visualizar productos con todas las funcionalidades esperadas en una aplicación de e-commerce.
