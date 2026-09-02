package com.homiq.app.data.backup

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

data class DriveBackupFile(
    val id: String,
    val name: String,
)

class GoogleDriveBackupClient {
    fun findLatestBackup(
        accessToken: String,
    ): DriveBackupFile? {
        val query =
            "name = '$BACKUP_FILE_NAME' and trashed = false"
        val url =
            URL(
                "$DRIVE_API/files" +
                    "?spaces=appDataFolder" +
                    "&pageSize=10" +
                    "&orderBy=modifiedTime%20desc" +
                    "&fields=files(id%2Cname)" +
                    "&q=${
                        URLEncoder.encode(
                            query,
                            "UTF-8",
                        )
                    }",
            )
        val response =
            request(
                url = url,
                method = "GET",
                accessToken = accessToken,
            )
        val files =
            JSONObject(response)
                .optJSONArray("files")
                ?: JSONArray()

        if (files.length() == 0) {
            return null
        }

        val item = files.getJSONObject(0)
        return DriveBackupFile(
            id = item.getString("id"),
            name = item.getString("name"),
        )
    }

    fun download(
        fileId: String,
        accessToken: String,
    ): String =
        request(
            url = URL(
                "$DRIVE_API/files/$fileId?alt=media",
            ),
            method = "GET",
            accessToken = accessToken,
        )

    fun create(
        content: String,
        accessToken: String,
    ) {
        val boundary =
            "homika_backup_${System.nanoTime()}"
        val metadata =
            JSONObject()
                .put("name", BACKUP_FILE_NAME)
                .put(
                    "parents",
                    JSONArray().put(
                        "appDataFolder",
                    ),
                )
                .toString()

        val body = buildString {
            append("--")
            append(boundary)
            append("\r\n")
            append(
                "Content-Type: application/json; charset=UTF-8\r\n\r\n",
            )
            append(metadata)
            append("\r\n--")
            append(boundary)
            append("\r\n")
            append(
                "Content-Type: application/json; charset=UTF-8\r\n\r\n",
            )
            append(content)
            append("\r\n--")
            append(boundary)
            append("--\r\n")
        }

        request(
            url = URL(
                "$DRIVE_UPLOAD/files" +
                    "?uploadType=multipart" +
                    "&fields=id%2Cname",
            ),
            method = "POST",
            accessToken = accessToken,
            contentType =
                "multipart/related; boundary=$boundary",
            body = body,
        )
    }

    fun update(
        fileId: String,
        content: String,
        accessToken: String,
    ) {
        request(
            url = URL(
                "$DRIVE_UPLOAD/files/$fileId" +
                    "?uploadType=media" +
                    "&fields=id%2Cname",
            ),
            method = "PATCH",
            accessToken = accessToken,
            contentType =
                "application/json; charset=UTF-8",
            body = content,
        )
    }

    private fun request(
        url: URL,
        method: String,
        accessToken: String,
        contentType: String? = null,
        body: String? = null,
    ): String {
        val connection =
            url.openConnection()
                as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken",
            )
            connection.setRequestProperty(
                "Accept",
                "application/json",
            )

            if (
                contentType != null &&
                body != null
            ) {
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    contentType,
                )
                connection.outputStream
                    .bufferedWriter(
                        Charsets.UTF_8,
                    )
                    .use {
                        it.write(body)
                    }
            }

            val status =
                connection.responseCode
            val stream =
                if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val response =
                if (stream == null) {
                    ""
                } else {
                    BufferedReader(
                        InputStreamReader(
                            stream,
                            Charsets.UTF_8,
                        ),
                    ).use {
                        it.readText()
                    }
                }

            if (status !in 200..299) {
                throw DriveBackupHttpException(
                    statusCode = status,
                    responseBody = response,
                )
            }

            return response
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val BACKUP_FILE_NAME =
            "homika-backup-latest.homika.json"

        private const val DRIVE_API =
            "https://www.googleapis.com/drive/v3"
        private const val DRIVE_UPLOAD =
            "https://www.googleapis.com/upload/drive/v3"
    }
}

class DriveBackupHttpException(
    val statusCode: Int,
    val responseBody: String,
) : RuntimeException(
    "Drive backup HTTP $statusCode",
)
