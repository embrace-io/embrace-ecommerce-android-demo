# Sample Shopping - Lista de Productos con Jetpack Compose

## 🛍️ Funcionalidades Implementadas

### ✅ LazyGrid Implementation
- **LazyVerticalGrid** con columnas adaptativas
- Grid responsivo que se ajusta al tamaño de pantalla
- Optimización de rendimiento con lazy loading

### ✅ Filtrado Avanzado
- **Búsqueda por texto** en nombre y descripción
- **Filtrado por categorías** con chips seleccionables
- **Rango de precios** (precio mínimo y máximo)
- **Filtro de stock** (solo productos disponibles)
- **Filtro por rating** mínimo
- **Indicador visual** de filtros activos

### ✅ Capacidades de Ordenamiento
- **Bottom sheet** con opciones de ordenamiento
- **7 opciones de ordenamiento**:
  - Nombre A-Z / Z-A
  - Precio: Menor a Mayor / Mayor a Menor
  - Mejor Valorados (por rating)
  - Más Recientes
  - Más Populares (por número de reseñas)

### ✅ Paginación
- **Carga infinita** automática
- **Detección de scroll** al final de la lista
- **Indicadores de estado** (cargando más, fin de lista)
- **Prevención de cargas duplicadas**

### ✅ Funcionalidad de Búsqueda
- **Búsqueda en tiempo real**
- **Filtrado instantáneo** de resultados
- **Mensajes personalizados** para resultados vacíos
- **Integración con filtros** existentes

### ✅ Estados de Carga y Manejo de Errores
- **Pantalla de carga inicial**
- **Pull-to-refresh** para actualizar datos
- **Pantalla de error** con opción de reintentar
- **Estado vacío** con mensajes informativos
- **Indicadores de carga** para paginación

## 🏗️ Arquitectura

### Modelos de Dominio
```kotlin
data class Product(
    val id: String,
    val name: String,
    val priceCents: Int,
    val description: String,
    val imageUrl: String,
    val category: String,
    val rating: Float,
    val reviewCount: Int,
    val inStock: Boolean,
    val discountPercentage: Int
)

data class ProductFilters(
    val searchQuery: String,
    val selectedCategory: String?,
    val minPrice: Int?,
    val maxPrice: Int?,
    val inStockOnly: Boolean,
    val minRating: Float,
    val sortOption: SortOption
)
```

### Componentes UI Reutilizables
- **ProductCard**: Tarjeta de producto con imagen, precio, rating y badges
- **FilterBottomSheet**: Bottom sheet completo para filtros y ordenamiento
- **LoadingStates**: Componentes para estados de carga, error y vacío
- **FilterButton**: Botón de filtros con indicador de estado activo

### ViewModel
- **HomeViewModel** con estado completo de la UI
- **Manejo de filtros** y ordenamiento
- **Paginación** automática
- **Estados de carga** diferenciados

## 🎨 Características de UI/UX

### Diseño Moderno
- **Material Design 3** con temas dinámicos
- **Tarjetas elevadas** con sombras
- **Imágenes optimizadas** con Coil
- **Badges informativos** (descuentos, agotado)

### Interacciones
- **Pull-to-refresh** para actualizar
- **Scroll infinito** para cargar más
- **Filtros en tiempo real**
- **Navegación fluida**

### Estados Visuales
- **Loading states** con spinners
- **Error states** con opciones de retry
- **Empty states** con mensajes contextuales
- **Indicadores de progreso** para paginación

## 📱 Cómo Usar

### 1. Navegación
```kotlin
HomeScreen(
    onProductClick = { product -> 
        // Navegar al detalle del producto
    }
)
```

### 2. Filtros
- Toca el botón "Filtros" para abrir el bottom sheet
- Usa la barra de búsqueda para filtrar por texto
- Selecciona categorías con los chips
- Ajusta rangos de precio y rating

### 3. Ordenamiento
- Abre el bottom sheet de filtros
- Selecciona una opción de ordenamiento
- Los cambios se aplican automáticamente

## 🔧 Configuración

### Dependencias
Todas las dependencias necesarias están incluidas en `gradle/libs.versions.toml`:
- Jetpack Compose BOM
- Material Design 3
- Coil para imágenes
- Room para persistencia
- Hilt para inyección de dependencias

### Datos de Ejemplo
Los archivos `products.json` y `categories.json` contienen datos de ejemplo con:
- 12 productos variados
- 7 categorías diferentes
- Imágenes de Unsplash
- Ratings y reseñas realistas

## 🚀 Próximas Mejoras

- [ ] Implementar favoritos
- [ ] Añadir animaciones de transición
- [ ] Implementar búsqueda por voz
- [ ] Añadir filtros guardados
- [ ] Implementar modo oscuro/claro
- [ ] Añadir comparación de productos

## 📄 Licencia

Este proyecto es parte de la muestra de implementación de Jetpack Compose para aplicaciones de comercio electrónico.

## 🤖 Estrategia de Workflows Espresso
- **Build APKs (on change)** compila en cada push a `main` o `master` y publica los artefactos (`app-mockDebug.apk`, `app-mockSandbox.apk`, `app-mockEurope.apk`, `app-prodDebug.apk` más sus correspondientes `-androidTest.apk`).
- **Run Espresso (every 10 min, reuse last build)** se ejecuta cada 10 minutos o manualmente, descarga el último build exitoso de cada variante, inicia un emulador, instala los APKs precompilados y ejecuta `am instrument` sin volver a compilar.
