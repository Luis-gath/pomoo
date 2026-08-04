package com.example.pomodoro.features.auth.data

import android.content.Context
import android.util.Log
import com.example.pomodoro.R
import com.example.pomodoro.features.auth.domain.AuthResult
import com.example.pomodoro.features.auth.domain.AuthType
import com.example.pomodoro.features.auth.domain.AuthUser
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val appContext: Context
) : AuthRepository {

    /**
     * Lo genera el plugin google-services a partir de google-services.json. Leerlo aquí y
     * no recibirlo por parámetro evita que se quede sin asignar, que es justo lo que
     * ocurría antes: el identificador se declaraba vacío y nadie lo rellenaba nunca.
     */
    private val webClientId: String
        get() = appContext.getString(R.string.default_web_client_id)

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun currentUserOrNull(): AuthUser? = auth.currentUser?.toAuthUser()

    override suspend fun signInAnonymously(): AuthResult = runAuth {
        auth.signInAnonymously().await().user
    }

    override suspend fun signInWithGoogle(activityContext: Context): AuthResult =
        withGoogleCredential(activityContext) { credential ->
            auth.signInWithCredential(credential).await().user
        }

    override suspend fun linkAnonymousToGoogle(activityContext: Context): AuthResult {
        val user = auth.currentUser
            ?: return AuthResult.Error("No hay ninguna sesión activa que vincular")
        if (!user.isAnonymous) {
            return AuthResult.Error("Esta cuenta ya está vinculada")
        }

        return withGoogleCredential(activityContext) { credential ->
            user.linkWithCredential(credential).await().user
        }
    }

    override fun signOut() = auth.signOut()

    /**
     * Pide un token de Google con Credential Manager y se lo entrega a [onCredential].
     *
     * Credential Manager es la vía actual; `GoogleSignInClient` está obsoleto.
     */
    private suspend fun withGoogleCredential(
        activityContext: Context,
        onCredential: suspend (AuthCredential) -> FirebaseUser?
    ): AuthResult {
        val credentialManager = CredentialManager.create(activityContext)

        // Primero el acceso silencioso, que reconoce al que ya entró antes sin
        // enseñarle otra vez el selector.
        val seamless = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        // GetGoogleIdOption responde NoCredentialException cuando no encuentra una
        // credencial que encaje, incluso habiendo cuentas en el dispositivo. Para un
        // botón explícito hay que caer en GetSignInWithGoogleOption, que abre el
        // selector completo en lugar de rendirse.
        val explicit = GetSignInWithGoogleOption.Builder(webClientId).build()

        val response = try {
            credentialManager.request(activityContext, seamless)
        } catch (e: NoCredentialException) {
            Log.i(TAG, "Sin credencial reutilizable; abriendo el selector de cuentas")
            try {
                credentialManager.request(activityContext, explicit)
            } catch (e2: GetCredentialCancellationException) {
                return AuthResult.Cancelled
            } catch (e2: NoCredentialException) {
                Log.w(TAG, "El selector tampoco devolvió credencial", e2)
                return AuthResult.NoCredential
            } catch (e2: Exception) {
                Log.w(TAG, "Fallo al abrir el selector de cuentas", e2)
                return AuthResult.Error(e2.message ?: "No se pudo abrir el selector de cuentas")
            }
        } catch (e: GetCredentialCancellationException) {
            return AuthResult.Cancelled
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al pedir credencial de Google", e)
            return AuthResult.Error(e.message ?: "No se pudo abrir el selector de cuentas")
        }

        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return AuthResult.Error("Google devolvió una credencial inesperada")
        }

        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        return runAuth { onCredential(GoogleAuthProvider.getCredential(idToken, null)) }
    }

    private suspend fun CredentialManager.request(
        activityContext: Context,
        option: CredentialOption
    ) = getCredential(
        activityContext,
        GetCredentialRequest.Builder().addCredentialOption(option).build()
    )

    private suspend fun runAuth(block: suspend () -> FirebaseUser?): AuthResult = try {
        block()?.toAuthUser()?.let { AuthResult.Success(it) }
            ?: AuthResult.Error("Firebase no devolvió ningún usuario")
    } catch (e: Exception) {
        // Sin esta traza, un proveedor deshabilitado en la consola solo se manifiesta
        // como un mensaje fugaz en pantalla.
        Log.w(TAG, "Fallo de autenticación en Firebase", e)
        AuthResult.Error(e.message ?: "Error de autenticación")
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString(),
        type = when {
            isAnonymous -> AuthType.ANONYMOUS
            providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } -> AuthType.GOOGLE
            else -> AuthType.NONE
        }
    )

    private companion object {
        const val TAG = "AuthRepository"
    }
}
