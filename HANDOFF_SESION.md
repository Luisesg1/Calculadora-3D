# Handoff — Cotiza 3D: Calculadora (continuar en otra sesión)

App Android (Kotlin/Compose/Hilt/Room) para cotizar impresiones 3D. Foco LatAm, es/en/pt.
Ruta proyecto: `C:\Users\luise\Calculadora 3D`

## Estado git (al día de este handoff)
- Rama `master`, **1 commit** con todo el CRM+inventario: `7db94b8 feat: CRM de cotizaciones + inventario automático`.
- Local == `origin/master` (mismo hash). Todo subido a GitHub (`Luisesg1/Calculadora-3D`).
- Working tree limpio.

## Build / correr
- **JDK:** usar JBR 21 de Android Studio (el sistema tiene JDK 25, rompe Gradle):
  `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`
- Compilar: `JAVA_HOME="..." ./gradlew :app:compileDebugKotlin` (rápido) o `:app:assembleDebug`.
- Instalar en emulador: `./gradlew :app:installDebug`.
- KSP2 off (`ksp.useKSP2=false` en gradle.properties) — mantener.
- ADB: `/c/Users/luise/AppData/Local/Android/Sdk/platform-tools/adb.exe`, emulador `emulator-5554`.
  - Para rutas /sdcard en Git Bash usar `MSYS_NO_PATHCONV=1` (si no, mangla la ruta).
- Emulador: AVD `Medium_Phone_API_36.1`. Lanzar: `emulator -avd Medium_Phone_API_36.1 -no-snapshot-load`.

## ⚠️ Keystore (CRÍTICO — respaldar)
- `cotiza3d-upload.jks` (raíz del repo), alias `upload`. Contraseña en `keystore.properties` (raíz).
- Firma en `app/build.gradle.kts` (signingConfigs.release lee keystore.properties).
- **Perderlo = no más updates en Play.** Respaldar .jks + password fuera del PC.
- Release AAB: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.

## Versión
- **versionCode 3, versionName 1.0.2** (subido este trabajo; antes era 2 / 1.0.1).

---

## LO NUEVO ESTA SESIÓN: CRM + Inventario automático (ya en master, probado en emulador)

Extiende la arquitectura existente. **CalculationEngine intacto** (fuente de verdad). **Sin tablas Supabase nuevas** (el snapshot de CloudSync amplía a las tablas nuevas; RLS por user_id de `user_backups` ya cubre todo). Room **v5 → v6** (`MIGRATION_5_6`, patrón ALTER + defaults, no borra datos).

### CRM de cotizaciones
- `QuoteStatus` (domain/model/Enums.kt) = 8 estados: DRAFT, SENT, VIEWED, ACCEPTED, IN_PRODUCTION, DELIVERED, REJECTED, CANCELLED. Helpers `KANBAN_FLOW`, `next()`, `isTerminal`.
- `Quotation` (Models.kt / QuotationEntity): + `dueDate`, `updatedAt`, `stockDeducted`, timestamps por estado (sentAt…deliveredAt). Vencimiento **derivado** `dueState()` (VIGENTE / POR_VENCER ≤3d / VENCIDA) — nunca muta el estado.
- Tabla **`quote_events`** (id, quotationId, status, timestamp, note) = auditoría/timeline. `QuotationRepository.updateStatus()` estampa el timestamp, registra evento, y **si pasa a IN_PRODUCTION y el user es Pro → dispara `InventoryManager.applyConsumption`**.
- `Client` gana `rut`.
- **Sección Cotizaciones** = `feature/history/HistoryScreen.kt` (reusa la ruta `history`): filtros estado/cliente/fecha, orden por fecha, badges de vencimiento, ganancia, **toggle Lista ↔ Kanban (Pro)**.
- Pantallas nuevas:
  - `feature/quotedetail/` — detalle: montos, cliente (link a ficha), fecha de vencimiento (DatePicker), PDF/PNG desde cotización guardada, botón "iniciar producción", **timeline** de `quote_events`.
  - `feature/clientdetail/` — ficha: total comprado, nº cotizaciones, aceptadas/rechazadas, historial. Acceso desde detalle y desde `ClientsScreen` (tap en la card).
- Rutas nuevas en `navigation/` : `quote/{quoteId}`, `client/{clientId}`. History/Home abren el detalle; editar sigue yendo a la calculadora.

### Inventario automático
- `Material` (Models.kt / MaterialEntity): + `currentWeightG` (restante, backfill = spool), `minStockG`, `purchaseDate`. Derivados `stockPct`, `stockStatus` (OK/LOW/OUT).
- Tabla **`material_movements`** (ledger: delta ±, `MovementReason` COMPRA/CONSUMO/CORRECCION/MERMA/DEVOLUCION/AJUSTE, prev/new, quotationId).
- **`InventoryManager`** (data/repo/Repositories.kt):
  - `applyConsumption(quote)` — descuenta `line.grams` por cada MaterialLine, crea movimiento CONSUMO con quotationId, marca `stockDeducted=true`. **Guard: no descuenta dos veces** (probado en vivo: reentrar a producción no vuelve a descontar).
  - `adjustStock(materialId, delta, reason, note)` — ajuste manual (+/−).
- `feature/materials/`: barra de stock en cada card, sección **"Materiales con stock bajo"**, diálogo de ajuste (+/− + motivo + lista de movimientos). Editor con Stock actual + Stock mínimo.

### Stats + Dashboard
- `StatsViewModel/StatsData` extendido: creadas, aceptadas, rechazadas, **conversión**, pendientes, vencidas, en producción, ventas/ganancia de cotizaciones, material más usado / mayor consumo / costo consumido. **Avanzadas = Pro** (gate en StatsScreen; free ve un hint).
- Home (`HomeViewModel/HomeScreen`) tiene card **dashboard**: pendientes / en producción / stock bajo + ingresos y ganancia del mes.

### Monetización (Free vs Pro)
- `MonetizationViewModel`: `FREE_QUOTES_PER_MONTH=15` (antes era cap total de 10), `FREE_MATERIAL_LIMIT=5`, `FREE_TEMPLATE_LIMIT=1`.
- FREE: CRM completo (8 estados), 15 cotizaciones/mes, 5 materiales, inventario **manual**, stats básicas.
- PRO: ilimitado + **descuento automático de stock** + **Kanban** + **stats avanzadas** + sin ads + sync (ya existía).
- Gate del límite de materiales en `MaterialsScreen` (FAB bloqueado → diálogo upsell). Gate de cotizaciones/mes en `CalculatorScreen` (`vm.savedThisMonth()`).
- `ProUpsellCard` menciona los nuevos beneficios (inventario, CRM/Kanban).

### CloudSync
- `BackupData` incluye `movements` y `quoteEvents`. `push/pull/clearLocal` actualizados. Los 2 nuevos DAOs tienen `getAllOnce/insertAll/clearAll`. **Sin cambios en Supabase.**

### Fix incluido
- Bug de locale en `StockAdjustDialog` (AlertDialog salía en inglés): se hoistean TODOS los `stringResource` fuera del AlertDialog. Mismo patrón que el resto de popups. (Ver también `locale-hilt-crash` en memoria.)

---

## Modelo de monetización previo (sigue vigente)
| | Cotizar | Guardar | Ads | Logo/PDF limpio | Auto-inventario | Kanban | Stats avanzadas |
|---|---|---|---|---|---|---|---|
| Invitado (sin cuenta) | 3/día | 10 | sí | no | no | no | no |
| Cuenta gratis | 15/mes | ∞ (cap mensual aplica al crear) | sí | no | no | no | no |
| **Pro** (sub) | ∞ | ∞ | no | sí | sí | sí | sí |

- Ads: interstitial cada 5 pantallas (`SCREENS_PER_AD`), disparado en AppNavGraph. **Probado en vivo** (sale el test ad de AdMob).
- Archivos clave: `data/ads/InterstitialAdManager.kt`, `data/billing/BillingRepository.kt` (`PRODUCT_ID="remove_ads"`, base plans `monthly`/`annual`), `feature/monetization/*`, `data/quota/GuestQuotaManager.kt`.

## Estado Google Play / AdMob (de sesiones previas — verificar)
- App en Play Console (cuenta Ind-Corp), package `com.print3d.calculator`, Prueba interna activa. Suscripción `remove_ads` con base plans monthly/annual.
- AdMob: App ID `ca-app-pub-4175062533825097~7704459108`, Interstitial `ca-app-pub-4175062533825097/8985751184` (reales en release, TEST en debug).
- **PENDIENTES:** subir AAB nuevo (versionCode 3), completar Payments en AdMob, confirmar precios de los base plans.

---

## Verificado en emulador esta sesión
Migración v5→v6 sobre datos reales sin wipe · crear material con stock + alerta stock bajo · ajuste manual (−200) · crear cotización (CalculationEngine OK) · ciclo de estados con timeline · **descuento automático 800→680g al producir** (con Pro) · **no doble descuento** al reentrar · dashboard en Home · interstitial ad. `assembleDebug` OK.

**NO probado en vivo:** auto-descuento en build debug normal (requiere Pro/billing real de Play — se forzó Pro temporalmente para verificar y se revirtió).

## Pendientes / ideas futuras
1. Subir AAB versionCode 3 a Prueba interna.
2. "Vista" automática por enlace público web (se dejó manual esta sesión — falta backend/hosting + tabla pública Supabase).
3. Fecha de compra del material: hoy se auto-setea al crear; no hay editor de fecha explícito.
4. Sync per-row (`user_id`+`updated_at` por fila) para multi-dispositivo fino; hoy es snapshot last-write-wins por cuenta.
5. Export CSV/Excel del historial; T&C en PDF.

## Memoria del proyecto
`~/.claude/projects/C--Users-luise-Calculadora-3D/memory/` — ver `crm-inventory.md` (esta feature), `monetization.md`, `cloud-backup.md`, `build-env.md`, `locale-hilt-crash.md`.
