# Handoff — Nube / Auth (Calculadora 3D)

Documento para continuar en otro chat. Trabajo de esta sesión: **2026-08-11**.
Complementa `HANDOFF.md` (estado general 2026-08-02) y `PROJECT_STATE.md`.

## Qué se hizo esta sesión
Se pasó de "solo local" a **datos en la nube al iniciar sesión**. Tres piezas, todas probadas en vivo:

1. **Perfil al login** (`profiles`)
2. **Respaldo completo de datos** (`user_backups`, snapshot)
3. **Google Sign-In** (Credential Manager nativo)

Auditoría original resuelta: al iniciar sesión se guarda perfil + respaldo completo del dataset.

## Build (recordatorio)
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :app:assembleDebug
```
- APK de instalación real: `app/build/intermediates/apk/debug/app-debug.apk` (el de `outputs/` no siempre existe; es test-only → instalar con `adb install -r -t`).
- KSP2 off. supabase-kt fijado 2.6.1.

## 1. Perfil al login — `profiles`
- `data/auth/ProfileRepository.kt`: `syncCurrentUser()` hace upsert `{id=auth.uid, email}`.
- Llamado desde `AuthRepository.postSignIn()` tras cualquier login exitoso.
- Tabla + RLS: `supabase/profiles.sql` (ya corrido en Supabase).

## 2. Respaldo completo — `user_backups`
- Modelo: **snapshot last-write-wins por cuenta** (todo el dataset → 1 JSON). Sin migración Room, sin mapear UUIDs.
- `data/sync/CloudSync.kt`:
  - `push()`: junta las 5 tablas (`getAllOnce`), serializa `BackupData`, upsert a `user_backups`. Devuelve Boolean.
  - `pull()`: baja fila, deserializa, **reemplaza** Room (clear + insertAll).
  - `clearLocal()`: limpia Room (usado en signout).
  - Observer: combina los 5 `observeAll()`, `debounce(2500)`, auto-push cuando hay sesión. Echo-guard `suppressPushUntil` para no subir snapshot vacío durante pull/clear.
- Entidades Room ahora `@Serializable`. DAOs ganaron `getAllOnce()/insertAll()/clearAll()`.
- Enganches en `AuthRepository`:
  - signIn/signUp exitoso → `pull()` (restaura).
  - signOut → `push()`; **si push OK** → `auth.signOut()` + `clearLocal()`. Offline (push falla) = NO limpia (no pierde datos).
- Tabla + RLS: `supabase/user_backups.sql` (ya corrido).
- Copy del diálogo signout actualizado (string `confirm_sign_out_msg` ES/EN/PT): ahora dice que los datos están en la nube.

## 3. Google Sign-In
- Nativo con **Credential Manager** (no navegador) → `auth.signInWith(IDToken)`.
- `data/auth/GoogleSignInHelper.kt`: `GetGoogleIdOption` + nonce (rawNonce UUID, hash SHA-256 al request; rawNonce a Supabase). Cancelación → `GoogleSignInCancelled` (UI la ignora).
- `AuthRepository.signInWithGoogle(idToken, rawNonce)` → mismo `postSignIn()`.
- `AuthViewModel.signInWithGoogle(context)` (context = Activity). Botón en `AuthScreen` (blanco, logo G 4 colores `res/drawable/ic_google_logo.xml`, texto oscuro — estilo oficial Google).
- Deps: `androidx.credentials` + `credentials-play-services-auth` 1.3.0, `googleid` 1.1.1.
- `BuildConfig.GOOGLE_WEB_CLIENT_ID` desde `local.properties`.

### Config externa (ya hecha, cuenta Google "A")
- Google Cloud proyecto `Print3D Calculator`:
  - OAuth consent (External), 2 test users.
  - Client **Android**: package `com.print3d.calculator` + SHA-1 debug `35:EC:4C:E2:05:FC:75:E0:D0:5E:25:19:07:EE:E1:20:F3:E3:CB:7F`.
  - Client **Web** (su id va a local.properties): `713787045138-h3h4dsm8v7g15gr0tvon6becg0vka4gb.apps.googleusercontent.com`.
- Supabase → Auth → Providers → Google ON (Client ID Web+Android, Secret).

## Estado de pruebas (en vivo)
- ✅ Email/password: crear cuenta → `profiles`; crear material → `user_backups`; signout limpia Room; re-login OK (pull). Verificado en Supabase Table Editor.
- ✅ Google login: **probado en teléfono físico, funciona**.

## Pendiente / futuro (opcional, nada urgente)
- **Release:** registrar SHA-1 del keystore de **release** en Google Cloud (client Android); publicar la app OAuth (sacar de modo Prueba) para usuarios fuera de la lista de test.
- **Sync per-row** (`user_id`+`updated_at` por fila) para multi-dispositivo fino en vez de last-write-wins por cuenta.
- **Merge en login** en vez de reemplazo (hoy: entrar a cuenta con backup existente pisa data guest local).
- Auto-pull en restauración de sesión (hoy solo login explícito hace pull) — evaluar riesgo de pisar cambios offline antes de hacerlo.

## Memoria del proyecto
`~/.claude/projects/C--Users-luise-Calculadora-3D/memory/` — nuevos: `profile-sync.md`, `cloud-backup.md`, `google-signin.md`. Índice en `MEMORY.md`.
