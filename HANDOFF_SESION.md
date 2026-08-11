# Handoff — Cotiza 3D: Calculadora (continuar en otra sesión)

App Android (Kotlin/Compose/Hilt/Room) para cotizar impresiones 3D. Foco LatAm, es/en/pt.
Ruta proyecto: `C:\Users\luise\Calculadora 3D`

## Build / correr
- **JDK:** usar JBR 21 de Android Studio (el sistema tiene JDK 25, rompe Gradle):
  `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`
- Debug: `./gradlew :app:installDebug` (instala el APK correcto, NO usar el de `intermediates/apk`)
- Release AAB: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
- KSP2 off (`ksp.useKSP2=false` en gradle.properties) — mantener.
- ADB: `/c/Users/luise/AppData/Local/Android/Sdk/platform-tools/adb.exe`, emulador `emulator-5554`.

## ⚠️ Keystore (CRÍTICO — respaldar)
- `cotiza3d-upload.jks` (raíz del repo), alias `upload`
- Contraseña en `keystore.properties` (raíz): `Cotiza3D_1786135297_Key!`
- Firma configurada en `app/build.gradle.kts` (signingConfigs.release lee keystore.properties)
- **Perderlo = no más updates en Play.** Respaldar .jks + password fuera del PC.

## Versión actual
- versionCode **2**, versionName **1.0.1** (en build.gradle.kts)
- **AAB v2 firmado ya generado** (con TODO lo de abajo). Falta subirlo a Play (Prueba interna → Crear versión nueva).

---

## Estado en Google Play Console (cuenta Ind-Corp)
- App creada: **Cotiza 3D: Calculadora**, package `com.print3d.calculator`
- **Prueba interna** activa; V1 subido (AAB viejo, versionCode 1). **Falta subir AAB v2.**
- License tester: `luiseduardooo2000@gmail.com` agregado (compras de prueba sin cobro real).
- Suscripción creada: producto **`remove_ads`** con 2 base plans **Activos**:
  - `monthly` (mensual)
  - `annual` (anual)
  - PENDIENTE confirmar/ajustar **precios**: mensual 1.900 CLP, anual 19.000 CLP.

## Estado AdMob (IDs reales cableados)
- App AdMob creada, ad unit **Interstitial** creado.
- IDs por build type en `app/build.gradle.kts`: DEBUG=TEST, RELEASE=REALES.
  - App ID real: `ca-app-pub-4175062533825097~7704459108`
  - Interstitial real: `ca-app-pub-4175062533825097/8985751184`
- **PENDIENTE:** completar perfil de **Payments** en AdMob (para cobrar).

---

## Modelo de monetización (implementado)
| | Cotizar | Guardar (Historial) | Ads | Logo/PDF limpio |
|---|---|---|---|---|
| Invitado (sin cuenta) | 3/día (reset 24h) | 10 | sí | no |
| Cuenta gratis | ∞ | 10 | sí | no |
| **Pro** (sub) | ∞ | ∞ | no | sí |

- Ads: **interstitial cada 5 pantallas** para no-suscritos (`MonetizationViewModel.SCREENS_PER_AD`, disparado en `AppNavGraph`).
- Suscripción quita ads + logo empresa en PDF + PDF sin marca de agua + cotizaciones ilimitadas.
- Límites: `FREE_QUOTE_LIMIT=10`, `FREE_TEMPLATE_LIMIT=1`, `GuestQuotaManager.DAILY_LIMIT=3`.
- Precios display por moneda: `AppCurrency.proMonthly` (anual = ×10 = "2 meses gratis").

## Archivos clave monetización
- `data/ads/InterstitialAdManager.kt` (AD_UNIT = `BuildConfig.ADMOB_INTERSTITIAL`)
- `data/billing/BillingRepository.kt` — `PRODUCT_ID="remove_ads"`, `BASE_PLAN_MONTHLY="monthly"`, `BASE_PLAN_ANNUAL="annual"`, `launchSubscribe(activity, basePlanId)`
- `feature/monetization/MonetizationViewModel.kt` + `findActivity(context)`
- `feature/monetization/ProUpsellCard.kt` — card "Hazte Pro" con selector mensual/anual (en Settings)
- `data/quota/GuestQuotaManager.kt` — cuota invitado (DataStore `guest_quota`)

---

## Lo hecho ESTA sesión (todo en el AAB v2)
1. **Rediseño UI premium:** fuente Inter bundled (`Type.kt`), `CardStyle` theme-aware (light sin border+sombra suave / dark border), tokens, EmptyState con CTA, identidad 3D (`Brand.kt` layerLines), press-scale en AppCard, `animateItem` en listas.
2. **Monetización completa** (tabla arriba): ads interstitial, suscripción mensual/anual, límites, watermark PDF, logo gateado.
3. **Plantillas de cotización** (Pro): tabla Room `templates` (migración v5), "+ Guardar/Usar plantilla" en calculadora. `FREE_TEMPLATE_LIMIT=1`.
4. **Estados de cotización** (Historial): enum `QuoteStatus` DRAFT/SENT/ACCEPTED/REJECTED, badge de color + menú, migración Room v4 (columna `status`).
5. **Agregar material/impresora inline** desde el selector de la calculadora ("+ Nuevo material" / "+ Nueva máquina") → se guardan en sus listas (Room) y quedan seleccionados. `MaterialEditorSheet` + nuevo `MachineEditorSheet` reutilizables. VM: `saveMaterial`, `saveMachine`.
6. **Fix bug idioma en popups:** AlertDialog / DropdownMenu / ModalBottomSheet caían al locale del device (inglés) ignorando el override in-app. Patrón de fix: **resolver `stringResource` FUERA del popup** (AlertDialog/menú) o **re-proveer `LocalContext`+`LocalConfiguration` dentro** (bottom sheets, ver `MaterialEditorSheet`/`MachineEditorSheet`).
7. **Icono + nombre:** icono adaptativo = tile azul del cubo 3D (foreground `res/drawable-nodpi/ic_launcher_foreground.png` al 70% + fondo azul `#284C8B` en `values/ic_launcher_background.xml`). `app_name` = **"Cotiza 3D: Calculadora"** (3 idiomas).
8. Base: DB Room v5, proguard `-dontwarn org.slf4j.**` para R8.

---

## PENDIENTES (próxima sesión, en orden)
1. **Subir AAB v2** a Prueba interna (Play Console → Prueba interna → Crear versión nueva → subir `app-release.aab`). Es lo que lleva todo lo de arriba al teléfono.
2. **Probar compra real** de la suscripción con el license tester (Ajustes → Hazte Pro → mensual/anual). Verificar que `isSubscribed=true` quita ads.
3. **AdMob Payments** — completar perfil de cobro.
4. Confirmar **precios** de los base plans en Play (1.900 / 19.000 CLP).
5. **Bug idioma pendiente:** los editores inline de **ClientsScreen** y **MachinesScreen** (ModalBottomSheet dentro de sus propias pantallas) probablemente aún salen en inglés en device inglés — aplicar el mismo fix de re-proveer contexto (o refactorizar a usar los EditorSheet reutilizables).
6. Opcional: editor de **cliente** inline en calculadora (hoy el cliente se guarda por nombre al guardar la cotización).
7. Ideas Pro futuras no hechas: respaldo nube (Supabase sync), panel de negocio (tasa conversión aprovechando estados), export CSV/Excel, T&C en PDF.
8. Distribución/ASO cuando se publique (título ya tiene keyword: "Cotiza 3D: Calculadora").

## Notas
- App usa Supabase (email/password) — creds en `local.properties`. Auth es opcional (no bloquea cotizar/exportar).
- Export PDF/PNG libre para todos (sin muro login). WhatsApp share dedicado se quitó (redundante con chooser).
- Memoria del proyecto en `~/.claude/.../memory/` (project-overview, build-env, monetization, etc.).
