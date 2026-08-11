# 3D Print Calculator — Estado del proyecto

App Android comercial para cotizar impresiones 3D. Este doc resume TODO para retomar en otra sesión.

## Stack
- Kotlin · Jetpack Compose · Material 3 · MVVM · Hilt · Room · DataStore · Navigation Compose
- Package raíz: `com.print3d.calculator`
- compileSdk 35 · minSdk 26 · Gradle 8.11.1 · AGP 8.7.3 · Kotlin 2.0.21

## Build (IMPORTANTE)
- **JDK del sistema es 25 → NO sirve** (Gradle 8.11 falla). Compilar con el JBR 21 de Android Studio:
  ```bash
  JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleDebug
  ```
- **KSP2 apagado** (`ksp.useKSP2=false` en gradle.properties) por bug "unexpected jvm signature V".
- `org.gradle.java.home` en gradle.properties apunta al JBR (para Android Studio).
- SDK: `C:\Users\luise\AppData\Local\Android\Sdk`. APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Si Gradle da "Corrupted cache": borrar `~/.gradle/caches/journal-1` y `~/.gradle/caches/8.11.1/transforms`, luego `./gradlew --stop`.
- Emulador para pruebas: AVD `Medium_Phone_API_36.1`. Se muere seguido; relanzar con `-no-snapshot-load`.

## Arquitectura
```
domain/
  model/        Enums (AppCurrency+detectDefault región→moneda, AppLanguage, AppThemeMode), Models (Material, Machine, Client, QuoteInput, QuoteResult, Quotation, CostLine)
  calc/         CalculationEngine (motor puro), CostKeys
  catalog/      Catalog (filamentos, marcas, printerBrands→modelos)
data/
  local/        Room: Entities, Daos, AppDatabase (v3), migraciones
  repo/         Repositories (Material/Machine/Quotation/Client) + Mappers (AppJson)
  settings/     SettingsRepository (DataStore) + AppSettings
di/             AppModule (Hilt: db, daos)
core/util/      CurrencyFormatter (centralizado), DateFormatter, RelativeTime, LocaleHelper
ui/theme/       Color/Theme/Type/Shape (indigo, claro+oscuro)
ui/components/  AppCard, AppTextField (required/isError/prefix), AutoCompleteField, ConfirmDialog, EmptyState, SectionHeader, ResultRow
feature/        onboarding, home, calculator, materials, machines, clients, history, stats, settings, export
navigation/     AppNavGraph (bottom nav + rutas), Destinations (Routes)
```

## Base de datos (Room v3)
- Tablas: `materials`, `machines` (con `brand`), `quotations` (input/result como JSON), `clients`.
- Migraciones: `MIGRATION_1_2` (columna brand en machines), `MIGRATION_2_3` (tabla clients). `fallbackToDestructiveMigration` de respaldo.
- Quotation guarda QuoteInput y QuoteResult serializados (kotlinx.serialization). QuoteInput tiene defaults → compatible hacia atrás.

## Features implementadas
### Onboarding (feature/onboarding) — 3 pasos, SIN cuenta
- Orden: **Idioma → Moneda → Impresora → entra a la app**.
- Idioma: cards con badge+nombre nativo+descripción (Automatic/Español (Latinoamérica)/English/Português (Brasil)). Auto usa idioma del dispositivo, fallback a ES (recursos default). `vm.setLanguage` en vivo.
- Moneda: **auto-detección por región** (`AppCurrency.detectDefault()` por `Locale.country`; CLP default). 8 monedas en cards. `vm.setCurrency` en vivo.
- Impresora: chips de marcas (Bambu Lab/Creality/Anycubic/Elegoo/Prusa/**Otro**) → modelos dinámicos (FilterChip). "Otro" = campo único "Nombre o modelo". "No encuentro mi impresora" = marca+modelo manual. **Multi-impresora** (lista con X). Botón **deshabilitado** hasta impresora válida.
- Progreso: "Paso X de 3" localizado + barra animada. Barra inferior fija (Atrás + Continuar/Comenzar ancho, escala al presionar).
- Persistencia: `setOnboardingDone(true)` al terminar. Reabrir NO repite onboarding.
- Botón final localizado: Comenzar/Get started/Começar.

### Home (feature/home)
- Saludo por hora, tarjeta stats con contadores animados (cotizaciones, ingresos, kg material, **clientes = cartera real**).
- CTA principal "Nueva cotización" (gradiente + líneas de capa 3D sutiles).
- Grid "Gestión": Historial, Materiales, Máquinas, Clientes, Estadísticas, Empresa, Configuración. (Export tile ELIMINADO.)
- "Actividad reciente": tarjeta "Última cotización" destacada + lista con divisores + empty state.
- Aparición escalonada (Stagger).

### Bottom Navigation
Inicio / Cotizaciones (history) / Materiales / Configuración. Rutas detalle (calculator, machines, clients, stats) ocultan la barra.

### Calculadora (feature/calculator)
- Motor `CalculationEngine`: material (con desperdicio), electricidad, máquina (hourCost o depreciación), fallos como multiplicador, mano de obra + extras, margen%, recargo%, descuento%, IVA%, precio manual override.
- Cliente: AutoCompleteField desde cartera; al elegir **autollena teléfono/correo** (para PDF). Al guardar → `ensureExists` crea el cliente.
- Requeridos: **Proyecto + Cantidad** (botón Guardar deshabilitado si falta proyecto).
- Export PDF/PNG + compartir (botones en el resultado).

### Materiales / Máquinas / Clientes (CRUD)
- Todos: editar (lápiz), duplicar (confirmación), borrar (confirmación destructiva) vía `ConfirmDialog`.
- Material: AutoCompleteField tipos de filamento + marcas; prefijo de moneda en precio; requeridos = nombre + peso(>0) + precio(>0).
- Máquina: campo **marca** propio (persistido) + marca→modelos autocompletar; requerido = nombre.
- Cliente: nombre requerido; teléfono/correo/dirección/notas. Se autocrea desde cotizaciones.

### Historial (feature/history)
Buscar, favorita, editar, duplicar (confirm), borrar (confirm).

### Estadísticas (feature/stats)
Filtro fechas (Este mes/3 meses/Año/Todo). **Ganancia neta** (verde/rojo) = ingresos − costos. Ingresos, Costos, Cotizaciones, Ticket promedio. Gráfico de barras mensual (verde/rojo).

### Configuración (feature/settings)
- **Cuenta** (local): si hay sesión → nombre+correo + "Cerrar sesión" (confirm, **resetea onboarding** para volver al login inicial). Si no → "Iniciar sesión" (hoja con toggle crear/iniciar).
- Tema (sistema/claro/oscuro), idioma (Auto/ES/EN/PT), moneda (8), empresa+logo, valores por defecto (electricidad/margen/IVA).

### i18n y moneda
- Strings ES (default) / EN (`values-en`) / PT (`values-pt`). Nombres de idioma como `translatable="false"`.
- `CurrencyFormatter.format(amount, currency)` centralizado — separadores por locale (CLP `$15.990`, USD `$15.99`, EUR `€15,99`). NO conversión de divisas.
- 8 monedas en `AppCurrency` (code, symbol, decimals, localeTag, displayName).

### Export (feature/export)
- `QuoteRenderer`: dibuja cotización estilo ERP (logo, empresa, cliente + contacto, tabla costos, total, notas, QR, pie) en Canvas. Compartido por PDF y PNG.
- `QuoteExporter`: PDF (PdfDocument), PNG (Bitmap 2x), compartir vía FileProvider (`${applicationId}.fileprovider`, paths en `res/xml/file_paths.xml`).
- `QrGenerator` (zxing).

## Cuenta / Nube (estado)
- **Auth real**: Supabase email/password (gotrue-kt). Sesión persistida en DataStore (`AuthSessionManager`).
- **Perfil**: al login/signup se hace upsert de fila en tabla `profiles` (`ProfileRepository`). SQL en `supabase/profiles.sql`.
- **Backup en nube**: `CloudSync` (`data/sync/`) sube snapshot completo del dataset (5 tablas → 1 JSON) a `user_backups`. `pull()` al login restaura Room; auto-push (debounce) al cambiar; push antes de signOut. Modelo **last-write-wins por cuenta**. SQL en `supabase/user_backups.sql`. Correr ambos SQL en Supabase.
- Room sigue como fuente local/offline; entidades ahora `@Serializable`.
- **Google Sign-In**: nativo (Credential Manager) → `auth.signInWith(IDToken)`. `GoogleSignInHelper` + botón en `AuthScreen`. Requiere `GOOGLE_WEB_CLIENT_ID` en local.properties + OAuth clients (Web+Android con SHA-1) en Google Cloud + provider Google ON en Supabase.
- **Pendiente**: sync per-row (`user_id`+`updated_at` por fila) para multi-dispositivo fino; merge en vez de reemplazo al login.

## Gotchas guardados (memoria)
- `~/.claude/projects/.../memory/build-env.md` — JBR 21, KSP2 off.
- `~/.claude/projects/.../memory/locale-hilt-crash.md` — `LocaleHelper.wrap` debe devolver `ContextWrapper(activity)`, no `createConfigurationContext`, o `hiltViewModel()` crashea.

## Pendientes / ideas
- Backend nube (Supabase) para login real + sincronización + respaldo tras desinstalar. **Necesita credenciales del usuario.**
- "Impresora predeterminada" en Configuración (no existe el concepto aún; se elige por cotización).
- Formato CLP con espacio `$ 15.990` (spec lo pedía; se dejó sin espacio por consistencia con USD/EUR).
- Icono launcher es placeholder vectorial.
- Para Google Play: política de privacidad (obligatoria), icono real, keystore de release, capturas/ficha.

## Verificado en vivo (emulador)
Onboarding completo (idioma+moneda auto+impresora custom), persistencia (reabrir no repite), CRUD + confirmaciones, cliente autollenado, PDF con contacto, stats con datos, sign in/out. Build `assembleDebug` OK.
