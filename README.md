# 3D Print Calculator

Aplicación Android profesional para calcular y cotizar impresiones 3D. Kotlin · Jetpack
Compose · Material 3 · MVVM · Room · DataStore · Hilt · Navigation Compose.

## Estado

Proyecto **funcional y compilable** (`assembleDebug` genera APK). Fundación completa lista
para seguir creciendo como producto comercial.

## Arquitectura

```
domain/        Modelos puros + motor de cálculo (CalculationEngine) sin dependencias de Android
data/
  local/       Room: entidades, DAOs, AppDatabase
  repo/        Repositorios + mappers entidad↔dominio
  settings/    DataStore (tema, idioma, moneda, empresa, valores por defecto)
di/            Hilt (AppModule)
core/util/     Formateo de moneda/fecha, LocaleHelper (idioma en caliente)
ui/            Theme (Material 3, claro/oscuro) + componentes reutilizables
feature/       dashboard · calculator · materials · machines · history · settings · export
navigation/    NavGraph con transiciones animadas
```

## Funciona hoy

- **Dashboard** con tarjetas (tile destacado + grid).
- **Calculadora** completa: trabajo, material, máquina, datos de impresión, ~11 costos,
  fallos/desperdicio, margen, IVA, descuento, recargo, precio manual. Resultado en vivo.
- **Motor de cálculo** puro y testeable ([CalculationEngine.kt](app/src/main/java/com/print3d/calculator/domain/calc/CalculationEngine.kt)).
- **Materiales** y **Máquinas**: CRUD + duplicar (bottom sheets).
- **Historial**: buscar, favoritas, duplicar, editar, eliminar. Persistente (Room).
- **Configuración**: tema claro/oscuro/sistema, idioma (ES/EN/PT), moneda (8), empresa + logo,
  electricidad, margen/IVA por defecto.
- **Exportar PDF y PNG** con diseño tipo ERP (logo, cliente, tabla, total, notas, QR, pie) +
  **compartir** por Android Share ([QuoteRenderer.kt](app/src/main/java/com/print3d/calculator/feature/export/QuoteRenderer.kt)).
- **i18n**: español (default), inglés, portugués — sin textos hardcodeados.
- **Multi-moneda**: CLP · USD · EUR · MXN · ARS · PEN · COP · BRL (formato local).

## Preparado para futuro (arquitectura ya lista)

Login/nube (Firebase/Supabase), clientes, inventario/stock, escáner QR/código de barras,
plantillas PDF, facturación, respaldo, widgets, WearOS, estadísticas.

## Compilar

Requiere Android Studio (SDK 35). **Importante sobre el JDK:**

- Gradle 8.11 **no** soporta JDK 25. Compila con el JBR que trae Android Studio (JDK 21).
- KSP2 está desactivado (`ksp.useKSP2=false` en `gradle.properties`) por un bug conocido.

Desde terminal (usando el JBR de Android Studio):

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleDebug
```

O simplemente abre el proyecto en Android Studio y pulsa Run.
