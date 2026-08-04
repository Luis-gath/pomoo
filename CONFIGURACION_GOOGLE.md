# Configuración de Google Cloud y suscripciones

Guía específica de este proyecto. Los identificadores que aparecen son los reales
del código: proyecto Firebase `pomodoro-66f42`, productos `premium_3m` y
`premium_lifetime` (definidos en `BillingManager.kt`).

---

## Parte 0 — Dos decisiones que bloquean todo lo demás

### 0.1 El applicationId actual no sirve

Hoy es `com.example.pomodoro`. **Google Play rechaza cualquier paquete que empiece
por `com.example`**, así que con ese identificador no puedes ni crear la ficha de
la app, ni vender suscripciones, ni publicar.

Hay que cambiarlo. La opción natural es `com.luis.pomodoro`, porque es justo lo que
ya espera tu `app/google-services.json` actual y lo que usaba la rama de respaldo.

En `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.luis.pomodoro"   // antes: com.example.pomodoro
    ...
}
```

**No hace falta tocar `namespace`.** Puede seguir siendo `com.example.pomodoro`: solo
afecta a dónde se generan `R` y `BuildConfig`, y cambiarlo obligaría a renombrar el
paquete en todos los archivos Kotlin. Firebase y Play miran el `applicationId`.

> Este cambio no lo he aplicado yo porque `app/build.gradle.kts` es uno de los
> archivos que tienes modificados sin commitear, y no quiero mezclarme con tu trabajo.

### 0.2 El sufijo `.debug` rompe login y facturación

El bloque `debug` añade `applicationIdSuffix = ".debug"`, así que tus builds de
desarrollo se instalan como `com.luis.pomodoro.debug`. Eso significa que, para Google,
**son otra aplicación distinta**:

- Google Sign-In fallará salvo que registres también esa variante en Firebase.
- La facturación **no funcionará en absoluto**, porque Play solo reconoce el paquete
  exacto que has subido a la consola.

Tienes dos caminos, y puedes combinarlos:

| Situación | Qué hacer |
|---|---|
| Desarrollo normal (login, foro) | Registrar también `com.luis.pomodoro.debug` en Firebase |
| Probar compras y suscripciones | Compilar sin sufijo: `./gradlew assembleDebug -PsameAppId` |

Recuerda ese `-PsameAppId`: sin él no vas a poder probar ni una compra.

---

## Parte 1 — Google Cloud / Firebase (login con Google)

No tienes que entrar en Google Cloud Console directamente. Firebase gestiona por ti el
proyecto de Cloud que hay debajo y crea los clientes OAuth cuando registras la huella
SHA-1. Solo irías a Cloud Console si más adelante quieres pedir permisos adicionales
(Calendar, Drive) o personalizar la pantalla de consentimiento.

### 1.1 Obtener las huellas SHA-1

```bash
./gradlew signingReport
```

Fíjate en la variante `debug`: te dará algo como
`SHA1: A1:B2:C3:...`. Apunta esa huella.

**Para producción no uses la huella de tu keystore.** Si activas Play App Signing
(lo normal, y obligatorio para apps nuevas), Google vuelve a firmar tu app con *su*
clave, y la huella que importa es la suya. La encuentras en:

> Play Console → tu app → **Prueba y versiones → Integridad de la aplicación →
> Firma de apps** → «Certificado de la clave de firma de la app» → SHA-1

Este es el error clásico: registrar la huella de la clave de subida y que el login con
Google funcione en debug pero falle en producción.

### 1.2 Registrar las apps en Firebase

En [Firebase Console](https://console.firebase.google.com/) → proyecto `pomodoro-66f42`
→ Configuración del proyecto → Tus apps.

Registra **una app Android por cada paquete** que vayas a usar:

1. `com.luis.pomodoro` — añade la SHA-1 de debug y, cuando publiques, la de Play App Signing.
2. `com.luis.pomodoro.debug` — solo si mantienes el sufijo; añade la SHA-1 de debug.

Puedes añadir varias huellas a la misma app; hazlo, no las sustituyas.

> **Cómo saber que ha funcionado:** al descargar el `google-services.json` nuevo, ábrelo
> y busca `oauth_client`. Si sigue siendo una lista vacía, la SHA-1 no se registró bien
> y el login con Google no va a funcionar. Tu archivo actual tiene `oauth_client: []`,
> que es exactamente el síntoma.

### 1.3 Habilitar el proveedor

Firebase Console → **Authentication → Sign-in method**:

- Activar **Google**.
- Activar **Anónimo** (el código lo usa para que los invitados puedan leer el foro).
- Activar **Correo/contraseña** si quieres el registro por email.

### 1.4 Descargar `google-services.json`

Descárgalo de nuevo y colócalo en `app/google-services.json`, sustituyendo el actual.
Está en `.gitignore`, así que no se sube al repositorio: es correcto, pero significa
que cada persona que clone el proyecto necesita el suyo.

### 1.5 Aplicar el plugin de Gradle (ahora mismo falta)

Sin esto, el `google-services.json` no se procesa: Firebase no arranca y no se genera
el recurso con el Web Client ID.

En `gradle/libs.versions.toml`:

```toml
[versions]
googleServices = "4.4.2"

[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

En el `build.gradle.kts` raíz:

```kotlin
plugins {
    alias(libs.plugins.google.services) apply false
}
```

En `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.google.services)
}
```

### 1.6 Leer el Web Client ID del recurso generado

El plugin genera automáticamente `R.string.default_web_client_id` a partir del
`google-services.json`. **Úsalo en lugar de copiar el ID a mano** desde Cloud Console:
copiado a mano se desincroniza en cuanto regeneres el archivo.

Hoy `AuthViewModel` tiene `var webClientId: String = ""` y nada se lo asigna nunca, así
que el login con Google siempre cortaba antes de empezar. Debe pasar a leerse del
contexto:

```kotlin
context.getString(R.string.default_web_client_id)
```

### 1.7 Comprobar

1. `./gradlew assembleDebug` debe compilar (si el plugin no encuentra el JSON, falla aquí).
2. Instala y pulsa «Iniciar sesión con Google».
3. Si sale `DEVELOPER_ERROR` o `ApiException: 10`, es SHA-1 o paquete mal registrados.

---

## Parte 2 — Suscripciones (Google Play Console)

### 2.1 Requisito previo: la app tiene que estar subida

Esto sorprende a mucha gente: **la facturación no funciona hasta que has subido al menos
una versión a un canal de pruebas**. No basta con crear los productos. Si te saltas este
paso, `queryProductDetailsAsync` devuelve una lista vacía y en la pantalla premium verás
«...» como precio para siempre.

Orden correcto:

1. Crear la app en Play Console con el paquete `com.luis.pomodoro`.
2. Activar **Play App Signing** y generar tu keystore de subida.
3. Subir un AAB firmado a **Prueba interna**.
4. Solo entonces crear los productos.

### 2.2 Configurar la firma de release

Ahora mismo el bloque `release` de `app/build.gradle.kts` **no tiene `signingConfig`**,
así que `assembleRelease` genera un artefacto sin firmar que Play no acepta.

Crea el keystore de subida:

```bash
keytool -genkey -v -keystore upload-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Guarda las credenciales en `keystore.properties` (añádelo a `.gitignore`, junto con el
`.jks` — que ya está ignorado) y referencia ese archivo desde `build.gradle.kts`. Nunca
metas la contraseña en el `build.gradle.kts`.

Genera el AAB con `./gradlew bundleRelease`.

### 2.3 Crear la suscripción `premium_3m`

Play Console → **Monetizar → Productos → Suscripciones** → Crear suscripción.

- **ID de producto: `premium_3m`** — tiene que coincidir exactamente con la constante
  `PRODUCT_ID_SUBSCRIPTION_3M` de `BillingManager.kt`. **No se puede cambiar después.**
- Nombre y descripción visibles para el usuario.

Dentro de la suscripción crea un **plan base**:

- Tipo: renovación automática.
- **Periodo de facturación: 3 meses.**
- Precio por país.

Si quieres prueba gratuita o precio de lanzamiento, se añaden como **ofertas** sobre ese
plan base. Ojo con esto: el código actual coge `subscriptionOfferDetails.firstOrNull()`
tanto para mostrar el precio como para lanzar la compra, así que en cuanto tengas más de
una oferta la elección es arbitraria. Hay que arreglarlo antes de publicar ofertas.

Deja el plan base y la suscripción **activos**.

### 2.4 Crear el producto único `premium_lifetime`

Play Console → **Monetizar → Productos → Productos integrados en la aplicación**.

- **ID: `premium_lifetime`** (constante `PRODUCT_ID_LIFETIME`).
- Tipo: producto único gestionado.
- Precio. Y **actívalo**.

### 2.5 Licencias de prueba (para no pagar de verdad)

Play Console → **Configuración → Pruebas de licencia** (a nivel de cuenta, no de app).

Añade ahí las cuentas de Gmail con las que vayas a probar. Esas cuentas ven las compras
como de prueba: no se cobra nada y las suscripciones se renuevan aceleradamente (una
suscripción de 3 meses caduca en minutos), lo cual va muy bien para probar renovación y
caducidad.

La cuenta debe estar además en la lista de testers del canal de prueba interna.

### 2.6 Probar

```bash
./gradlew assembleDebug -PsameAppId
```

Sin `-PsameAppId` el paquete sería `com.luis.pomodoro.debug` y Play no lo reconocerá.

Comprueba: que aparecen los precios reales (no «...»), que la compra se completa, que el
estado premium persiste tras reiniciar la app, y que al caducar la suscripción de prueba
el premium desaparece.

> Este último punto **hoy falla**. La caducidad se calcula como `purchaseTime + 90 días`
> fijos en vez de leer el periodo real, y nada revoca nunca la compra de por vida.
> Está detallado en la auditoría; hay que arreglarlo para que esta prueba tenga sentido.

---

## Parte 3 — Validación en servidor (pendiente de decidir)

Todo lo anterior deja el estado premium guardado **solo en un DataStore local**.
Cualquiera con root o con la app modificada se pone premium editando un archivo.

Cerrar eso requiere:

1. **Google Play Developer API** habilitada en Cloud Console, con una cuenta de servicio
   vinculada en Play Console (Usuarios y permisos), para consultar el estado real de una
   compra desde tu backend.
2. **RTDN (notificaciones para desarrolladores en tiempo real)** mediante un tema de
   Pub/Sub, para enterarte de renovaciones, cancelaciones y reembolsos en el momento en
   que ocurren, en lugar de adivinarlos.
3. Una **Cloud Function** que reciba esas notificaciones y escriba el estado en Firestore.

El punto 3 implica **plan Blaze**, que es la misma decisión pendiente que quedó del foro
(los contadores fiables y las notificaciones también la necesitan). Mientras no se tome,
lo razonable es arreglar los fallos de cliente, que hacen falta igual.

Cuando llegue ese momento, añade también `obfuscatedAccountId` al lanzar la compra, con
el UID de Firebase: es lo que permite vincular una compra a una cuenta y detectar
cuentas compartidas.

---

## Resumen del orden

1. Cambiar `applicationId` a `com.luis.pomodoro`.
2. `./gradlew signingReport` → apuntar SHA-1.
3. Registrar apps y huellas en Firebase; activar Google y Anónimo.
4. Descargar `google-services.json`; verificar que `oauth_client` **no** está vacío.
5. Aplicar el plugin `google-services` en Gradle.
6. Leer el Web Client ID de `R.string.default_web_client_id`.
7. Crear keystore de subida y configurar la firma de release.
8. Crear la app en Play Console y subir un AAB a prueba interna.
9. Crear `premium_3m` (con plan base de 3 meses) y `premium_lifetime`; activarlos.
10. Añadir cuentas a pruebas de licencia.
11. Probar con `-PsameAppId`.

Los pasos 1 al 6 son para el login. Del 7 al 11, para las suscripciones. El 1 y el 2 son
comunes y bloquean todo lo demás.
