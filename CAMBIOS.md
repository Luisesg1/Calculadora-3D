# Cambios — sesión 2026-08-11

Resumen de todo lo trabajado sobre la app **Cotiza 3D (Calculadora 3D)**.
Base: commit `90f3e13`. Cabeza: `774c1ac`. Rama: `master`.

---

## 1. Ganancia y margen

- **Margen por defecto: 40%** (antes 50%). Aplicado en `SettingsRepository`
  (valor por defecto y lectura de DataStore), `QuoteInput.marginPct` y `CalcForm`.
- **Ganancia mostrada como % del precio final** además del monto. Nueva
  propiedad `QuoteResult.profitPctOfPrice` (`profit / subtotal * 100`), igual
  que la barra de ganancia del calculador de referencia. La fila "Ganancia"
  del resultado muestra `$X (40%)`.
- El margen sigue siendo un **campo libre editable** — quien cotiza lo ajusta
  a su gusto.

## 2. Recargo por tipo de filamento (agregado y luego retirado)

- Se agregó un recargo % opcional sobre el costo de material según el tipo de
  filamento (PLA/PETG/ABS/resina…), con dropdown de presets + campo editable.
- **Retirado a pedido:** el costo de material queda solo con el precio real del
  rollo por gramo. Se eliminó `filamentSurchargePct` del modelo, el motor, el
  formulario, la UI y los strings.

## 3. Logo real en el login

- La pantalla de autenticación mostraba un cubo dibujado a mano (Canvas).
  Ahora usa el **icono real de la app**.
- **Crash corregido:** `painterResource` no puede cargar el XML de adaptive-icon
  (`IllegalArgumentException: Only VectorDrawables and rasterized asset types
  are supported`). Solución: componer a mano el fondo con
  `ic_launcher_background` + el PNG `ic_launcher_foreground` escalado (~1.5×
  para compensar el margen de seguridad del adaptive icon), recortado a
  cuadrado redondeado.

## 4. Internacionalización y tipografía del login

- Los strings del rediseño premium de autenticación faltaban en `values-en` y
  `values-pt`, por lo que en dispositivos EN/PT caían al español (idioma
  mezclado). Se **completaron las traducciones EN y PT** (email, taglines,
  marca, etc.).
- **Tipografía uniforme** en las 4 líneas del footer del login: la línea de
  caption ahora usa el mismo estilo que las demás taglines (bodyMedium, peso
  Medium, mismo color y `lineHeight`).
- Auditoría completa de claves: las **306 cadenas traducibles** están presentes
  en es/en/pt. Las 6 `lang_*` son endónimos `translatable="false"` (nombre de
  cada idioma en su propio idioma), correcto por diseño.

---

## Commits

| Commit | Descripción |
|--------|-------------|
| `f9f9e34` | feat: recargo por tipo de filamento y ganancia ajustable |
| `2aa9b93` | feat: rediseño premium de la pantalla de autenticación |
| `fd64e8c` | fix: recargo de filamento muestra un solo signo de porcentaje |
| `95ce66d` | feat: usar el icono real de la app como logo en el login |
| `e4ae59a` | feat: quitar el recargo por tipo de filamento |
| `e46e2fd` | fix: crash en login por logo — componer el adaptive icon a mano |
| `774c1ac` | fix(auth): traducciones EN/PT completas y tipografía uniforme |

## Archivos tocados (netos)

- `app/src/main/java/com/print3d/calculator/data/settings/SettingsRepository.kt`
- `app/src/main/java/com/print3d/calculator/domain/calc/CalculationEngine.kt`
- `app/src/main/java/com/print3d/calculator/domain/model/Models.kt`
- `app/src/main/java/com/print3d/calculator/feature/calculator/CalcForm.kt`
- `app/src/main/java/com/print3d/calculator/feature/calculator/CalculatorScreen.kt`
- `app/src/main/java/com/print3d/calculator/feature/auth/AuthScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en/strings.xml`
- `app/src/main/res/values-pt/strings.xml`

## Build

- Verificado con `compileDebugKotlin` en cada paso; login probado en emulador
  (es/en/pt) sin crashes.
- AAB release firmado generado: `app/build/outputs/bundle/release/app-release.aab`
  (`versionCode 2`, `versionName 1.0.1`).
  Para probar desde el AAB: subir a Play internal testing o usar `bundletool`.
