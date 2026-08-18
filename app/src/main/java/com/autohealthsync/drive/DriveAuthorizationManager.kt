package com.autohealthsync.drive

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

class DriveAuthorizationManager(context: Context) {
    private val client = Identity.getAuthorizationClient(context)
    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(Scopes.DRIVE_FILE)))
        .build()

    suspend fun authorize(): AuthorizationOutcome =
        client.authorize(request).await().toOutcome()

    fun completeAuthorization(data: Intent): AuthorizationOutcome =
        client.getAuthorizationResultFromIntent(data).toOutcome()

    private fun AuthorizationResult.toOutcome(): AuthorizationOutcome = when {
        hasResolution() -> AuthorizationOutcome.UserActionRequired(
            requireNotNull(pendingIntent) { "Authorization resolution was missing" },
        )
        !accessToken.isNullOrBlank() -> AuthorizationOutcome.Authorized(requireNotNull(accessToken))
        else -> AuthorizationOutcome.Unavailable("Google Drive did not return an access token")
    }
}

sealed interface AuthorizationOutcome {
    data class Authorized(val accessToken: String) : AuthorizationOutcome
    data class UserActionRequired(val pendingIntent: PendingIntent) : AuthorizationOutcome
    data class Unavailable(val reason: String) : AuthorizationOutcome
}

