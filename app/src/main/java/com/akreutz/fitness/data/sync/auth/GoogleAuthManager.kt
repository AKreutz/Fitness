package com.akreutz.fitness.data.sync.auth

import android.accounts.Account
import android.content.Context
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.akreutz.fitness.BuildConfig
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Drive scope limited to this app's own hidden folder - never the user's general Drive files. */
private const val DRIVE_APPDATA_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"

sealed interface SignInResult {
    data class SignedIn(val account: Account) : SignInResult
    data object NoCredential : SignInResult
    data class Error(val message: String?) : SignInResult
}

/**
 * Signs the user in with their Google account (via Credential Manager) and mints Drive
 * `appDataFolder`-scoped access tokens for [com.akreutz.fitness.data.sync.drive.
 * GoogleDriveDataSource] to use. Never requests broader Drive access than that one hidden,
 * per-app folder.
 */
class GoogleAuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    var signedInAccount: Account? = null
        private set

    var consentLauncher: ((Intent, CompletableDeferred<Boolean>) -> Unit)? = null

    /**
     * Tries a silent sign-in against a previously authorized Google account first, falling back
     * to the full account picker if none is found (e.g. first launch, or the user revoked access).
     */
    suspend fun signIn(): SignInResult {
        val filtered = trySignIn(filterByAuthorizedAccounts = true)
        if (filtered !is SignInResult.NoCredential) return filtered
        return trySignIn(filterByAuthorizedAccounts = false)
    }

    private suspend fun trySignIn(filterByAuthorizedAccounts: Boolean): SignInResult {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(filterByAuthorizedAccounts)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return SignInResult.Error("Unexpected credential type")
            }
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val account = Account(googleIdTokenCredential.id, "com.google")
            signedInAccount = account
            SignInResult.SignedIn(account)
        } catch (e: GetCredentialException) {
            if (filterByAuthorizedAccounts) SignInResult.NoCredential else SignInResult.Error(e.message)
        }
    }

    /**
     * A Drive `appDataFolder`-scoped access token for the signed-in account, or `null` if not
     * signed in. If the scope hasn't been granted yet, walks the user through the consent screen
     * (via [consentLauncher]) and retries once.
     */
    suspend fun getDriveAccessToken(): String? {
        val account = signedInAccount ?: return null
        return try {
            fetchToken(account)
        } catch (e: UserRecoverableAuthException) {
            val granted = requestConsent(e.intent ?: return null)
            if (!granted) return null
            fetchToken(account)
        }
    }

    private suspend fun fetchToken(account: Account): String? = withContext(Dispatchers.IO) {
        GoogleAuthUtil.getToken(context, account, DRIVE_APPDATA_SCOPE)
    }

    private suspend fun requestConsent(intent: Intent): Boolean {
        val launcher = consentLauncher ?: return false
        val result = CompletableDeferred<Boolean>()
        launcher(intent, result)
        return result.await()
    }
}
