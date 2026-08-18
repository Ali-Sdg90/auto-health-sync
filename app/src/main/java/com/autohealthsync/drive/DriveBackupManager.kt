package com.autohealthsync.drive

import com.autohealthsync.storage.AppStateStore
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class DriveBackupManager(
    private val authorizationManager: DriveAuthorizationManager,
    private val stateStore: AppStateStore,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun isAuthorized(): Boolean =
        authorizationManager.authorize() is AuthorizationOutcome.Authorized

    suspend fun hasBackup(date: LocalDate, fileName: String): Boolean = withContext(Dispatchers.IO) {
        val token = when (val outcome = authorizationManager.authorize()) {
            is AuthorizationOutcome.Authorized -> outcome.accessToken
            is AuthorizationOutcome.UserActionRequired -> throw DriveAuthorizationRequiredException()
            is AuthorizationOutcome.Unavailable -> throw DriveAuthorizationRequiredException(outcome.reason)
        }
        val state = stateStore.current()
        val folderId = state.driveFolderId ?: return@withContext false
        state.driveFileIds[date.toString()]?.let { if (fileExists(token, it)) return@withContext true }
        findFile(token, fileName, folderId) != null
    }

    suspend fun upload(
        date: LocalDate,
        fileName: String,
        contents: String,
    ): DriveUploadResult = withContext(Dispatchers.IO) {
        val token = when (val outcome = authorizationManager.authorize()) {
            is AuthorizationOutcome.Authorized -> outcome.accessToken
            is AuthorizationOutcome.UserActionRequired -> throw DriveAuthorizationRequiredException()
            is AuthorizationOutcome.Unavailable -> throw DriveAuthorizationRequiredException(outcome.reason)
        }
        val folderId = ensureFolder(token)
        val state = stateStore.current()
        val storedFileId = state.driveFileIds[date.toString()]
        val existingFileId = storedFileId?.takeIf { fileExists(token, it) }
            ?: findFile(token, fileName, folderId)?.id

        val fileId = if (existingFileId != null) {
            updateFile(token, existingFileId, contents)
            existingFileId
        } else {
            createFile(token, fileName, folderId, contents).id
        }
        DriveUploadResult(fileId, existingFileId != null)
    }

    private suspend fun ensureFolder(token: String): String {
        val state = stateStore.current()
        state.driveFolderId?.let { id ->
            if (fileExists(token, id)) return id
            stateStore.setDriveFolderId(null)
        }

        val existing = findFile(token, FOLDER_NAME, parentId = null, mimeType = FOLDER_MIME_TYPE)
        val folderId = existing?.id ?: createFolder(token).id
        stateStore.setDriveFolderId(folderId)
        return folderId
    }

    private fun fileExists(token: String, fileId: String): Boolean {
        val request = Request.Builder()
            .url("$API_BASE/files/$fileId?fields=id%2Ctrashed")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code == 404) return false
            response.requireSuccess()
            val file = json.decodeFromString<DriveFile>(response.body?.string().orEmpty())
            return file.trashed != true
        }
    }

    private fun findFile(
        token: String,
        name: String,
        parentId: String?,
        mimeType: String? = null,
    ): DriveFile? {
        val terms = mutableListOf(
            "name = '${name.escapeDriveQuery()}'",
            "trashed = false",
        )
        parentId?.let { terms += "'$it' in parents" }
        mimeType?.let { terms += "mimeType = '$it'" }
        val url = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("drive/v3/files")
            .addQueryParameter("q", terms.joinToString(" and "))
            .addQueryParameter("spaces", "drive")
            .addQueryParameter("fields", "files(id,name,mimeType,trashed)")
            .addQueryParameter("pageSize", "10")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return httpClient.newCall(request).execute().use { response ->
            response.requireSuccess()
            json.decodeFromString<DriveFileList>(response.body?.string().orEmpty()).files.firstOrNull()
        }
    }

    private fun createFolder(token: String): DriveFile {
        val metadata = buildJsonObject {
            put("name", FOLDER_NAME)
            put("mimeType", FOLDER_MIME_TYPE)
        }.toString()
        val request = Request.Builder()
            .url("$API_BASE/files?fields=id%2Cname%2CmimeType")
            .header("Authorization", "Bearer $token")
            .post(metadata.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return executeForFile(request)
    }

    private fun createFile(
        token: String,
        name: String,
        folderId: String,
        contents: String,
    ): DriveFile {
        val metadata = buildJsonObject {
            put("name", name)
            put("mimeType", JSON_MIME_TYPE)
            put("parents", kotlinx.serialization.json.buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(folderId)) })
        }.toString()
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody(JSON_MEDIA_TYPE))
            .addPart(contents.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val request = Request.Builder()
            .url("$UPLOAD_BASE/files?uploadType=multipart&fields=id%2Cname")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()
        return executeForFile(request)
    }

    private fun updateFile(token: String, fileId: String, contents: String) {
        val request = Request.Builder()
            .url("$UPLOAD_BASE/files/$fileId?uploadType=media")
            .header("Authorization", "Bearer $token")
            .patch(contents.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        httpClient.newCall(request).execute().use { it.requireSuccess() }
    }

    private fun executeForFile(request: Request): DriveFile =
        httpClient.newCall(request).execute().use { response ->
            response.requireSuccess()
            json.decodeFromString(response.body?.string().orEmpty())
        }

    private fun okhttp3.Response.requireSuccess() {
        if (isSuccessful) return
        val bodyText = body?.string()?.take(300)
        if (code == 401) throw DriveAuthorizationRequiredException()
        throw DriveApiException(code, bodyText)
    }

    companion object {
        const val FOLDER_NAME = "Auto: Health Data"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private const val JSON_MIME_TYPE = "application/json"
        private const val API_BASE = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
        private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()
    }
}

data class DriveUploadResult(val fileId: String, val updatedExisting: Boolean)

class DriveAuthorizationRequiredException(
    message: String = "Google Drive authorization requires user interaction",
) : SecurityException(message)

class DriveApiException(val statusCode: Int, detail: String?) : IOException(
    "Google Drive request failed ($statusCode)${detail?.let { ": $it" }.orEmpty()}",
)

@Serializable
private data class DriveFileList(val files: List<DriveFile> = emptyList())

@Serializable
private data class DriveFile(
    val id: String,
    val name: String? = null,
    val mimeType: String? = null,
    val trashed: Boolean? = null,
)

private fun String.escapeDriveQuery(): String = replace("\\", "\\\\").replace("'", "\\'")
