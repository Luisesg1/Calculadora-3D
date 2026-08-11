# Handoff — Calculadora 3D (3D Print Calculator)

Documento pa continuar en otro chat. Estado al **2026-08-02**.

## Qué es
App **Android nativa** de cotización de impresión 3D. **Jetpack Compose + Kotlin + Gradle**. NO es web (sin React/Tailwind/npm). Un solo módulo `:app`.

## Stack / build
- Kotlin 2.0.21, Compose (BOM 2024.12.01), Hilt, Room, DataStore, Coil, Navigation Compose.
- **JDK:** usar Android Studio JBR 21 (no el JDK 25 del sistema). En terminal:
  ```bash
  export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
  ./gradlew :app:assembleDebug
  ```
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Mantener **KSP2 off**.

## Verificar en emulador (flujo usado)
adb en `/c/Users/luise/AppData/Local/Android/Sdk/platform-tools/adb.exe`.
```bash
A=/c/Users/luise/AppData/Local/Android/Sdk/platform-tools/adb.exe
$A install -r app/build/outputs/apk/debug/app-debug.apk
$A shell am start -n com.print3d.calculator/.MainActivity
$A exec-out screencap -p > shot.png
# reset a onboarding: $A shell pm clear com.print3d.calculator
```
**Emulador (Medium_Phone_API_36.1) se cae seguido (ANR del sistema).** Si no hay device, pedir al usuario abrirlo desde **Android Studio** (lanzarlo por CLI ha fallado). Extraer PDF generado: `$A exec-out run-as com.print3d.calculator cat cache/shared/COT-0001.pdf > out.pdf`.

## Auth (Supabase)
- Email/password real. `supabase-kt` **fijado 2.6.1** (3.x usa Kotlin 2.4, incompatible). Paquete imports `io.github.jan.supabase.gotrue.*`. Ktor 2.3.12.
- Credenciales en `local.properties`: `SUPABASE_URL`, `SUPABASE_ANON_KEY` → `BuildConfig`. Ya configuradas (proyecto `cjbmvogrijpktgkdwgrb`). "Confirm email" apagado en dashboard.
- Workaround: `configurations.all { resolutionStrategy { force("androidx.browser:browser:1.8.0") } }` (evita subir AGP).
- Gate: crear cotización es libre; **exportar (PDF/PNG) exige sesión** → diálogo → ruta `AUTH`. Ver `AppNavGraph.kt`.
- Archivos: `data/auth/{AuthRepository,AuthSessionManager}.kt`, `di/SupabaseModule.kt`, `feature/auth/{AuthScreen,AuthViewModel}.kt`. Errores tipados (`AuthError` enum) localizados en UI.

## Multi-material
- `QuoteInput.materialLines: List<MaterialLine>` (+ helpers `effectiveMaterialLines`, `totalGrams`). `@Serializable` guardado como `inputJson` (Room) → **sin migración**. Cotizaciones viejas caen al material único.
- Motor: `CalculationEngine.calculate(input, materialLookup, machine, rate)` suma por línea. UI: líneas dinámicas (material + gramos + X) + "Agregar material". `Material.displayLabel` = "nombre · color".

## PDF de cotización
- **HTML/CSS renderizado con WebView print** (vectorial A4), no Canvas. `feature/export/QuoteHtml.kt` + `HtmlToPdf.kt` + **`android/print/PdfWriter.java`** (vive en paquete `android.print` a propósito: constructores de callbacks son package-private).
- Diseño minimalista Stripe/Linear: 2 columnas, banda TOTAL azul, bloque centrado vertical, pie horizontal + QR. `exportPdf(...)` es **suspend**, recibe `materialName` (unido con " + " si varios).
- **PNG sigue con Canvas viejo** (`QuoteRenderer.kt`) — no migrado.
- Campos que NO existen en el modelo (omitidos): RUT, Ciudad, Región del cliente, imagen de producto.

## Design system (en progreso — FASE 1+2 hechas)
Objetivo: look premium unificado (Linear/Stripe/Vercel), reusando componentes.
- **Tokens** `ui/theme/Dimens.kt`: `Spacing` (xs–xxl), `Radius` (sm/md/lg/xl/pill), `Elevation` (card/raised/overlay).
- **Colores** `ui/theme/{Color,Theme}.kt`: esquema Indigo + `AppColors.success/warning/border` (CompositionLocal, light/dark). `Warning` nuevo.
- `AppShapes` alineado a tokens (cards 18, inputs 14, diálogos 24) → todos los `MaterialTheme.shapes.*` consistentes.
- **Cards unificadas** (borde fino + sombra `Elevation.card` + `shapes.medium`): dashboard stats/tiles (`HomeComponents.kt`), cotización reciente/empty (`HomeRecent.kt`), `StatsScreen.kt`. Materiales/Máquinas/Clientes/Historial ya usaban `AppCard` (canónico en `ui/components/Components.kt`).

### Pendiente design system (próximas pasadas)
- Estados focus/error más marcados en inputs (`AppTextField`).
- Micro-animaciones sutiles consistentes (press/scale) en más controles.
- Auditoría fina modo oscuro + espaciados por pantalla usando tokens (`Spacing`).
- Chips/tabs/tooltips → radios menores a `Radius.sm`.
- Onboarding cards y BottomBar: alinear radios a tokens.

## Otros fixes ya aplicados esta sesión
- Startup: si hay sesión va directo al dashboard (settings iniciaba en `null`, splash sostenido). `MainActivity.kt`, `SettingsViewModel.kt`.
- Onboarding steps 1/2: selección de card ya no se ve "turbia".
- Onboarding "Continuar" sobre barra de gestos (`navigationBarsPadding`).
- Home: ya no re-anima items al scrollear (Stagger anima una vez, `LocalPlayedStagger`).
- Campos numéricos filtran no-dígitos (`AppTextField` `sanitizeNumeric`).
- Tooltips "?" en campos no obvios (`HelpTooltip`).
- Costos adicionales colapsable; orden Material = máquina→material.
- Guardar cotización → Toast "Cotización guardada". Guardar Empresa → botón (dirty) + diálogo confirmación + snackbar.
- Chips de Gestión mismo tamaño (altura fija 132dp).
- Correo en Ajustes con elipsis; nav/label "Ajustes" (antes "Configuración") en ES/PT.
- i18n: login/errores auth + strings nuevos en ES/EN/PT.

## BUG conocido pendiente
Al exportar sin sesión → login → volver, **el form de cotización se borra** (`form` usa `remember`, no sobrevive navegación a `AUTH`). Fix: hoistear el form a `CalculatorViewModel` (SavedStateHandle) o `rememberSaveable` con Saver de `CalcForm`. Hay chip de tarea creado.

## i18n
3 idiomas: `res/values/` (ES), `values-en/`, `values-pt/`. Agregar strings a los 3 siempre.

## Memoria del proyecto
`~/.claude/projects/C--Users-luise-Calculadora-3D/memory/` — ver `MEMORY.md` (índice): project-overview, build-env, locale-hilt-crash, supabase-auth, multi-material, pdf-export.
