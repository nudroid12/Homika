package com.homiq.app.data.sync

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

data class DriveSyncFile(
    val id: String,
    val name: String,
)

class GoogleDriveRestClient {
    fun listSyncFiles(
        accessToken: String,
    ): List<DriveSyncFile> {
        val query =
            "name contains '$FILE_PREFIX' and trashed = false"

        val url =
            URL(
                "$DRIVE_API/files" +
                    "?spaces=appDataFolder" +
                    "&pageSize=100" +
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

        return List(files.length()) { index ->
            val item =
                files.getJSONObject(index)
            DriveSyncFile(
                id = item.getString("id"),
                name = item.getString("name"),
            )
        }.filter {
            it.name.startsWith(FILE_PREFIX)
        }
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
        fileName: String,
        content: String,
        accessToken: String,
    ) {
        val boundary =
            "homiq_${System.nanoTime()}"

        val metadata =
            JSONObject()
                .put("name", fileName)
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
                throw DriveHttpException(
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
        const val FILE_PREFIX =
            "homiq-sync-device-"

        private const val DRIVE_API =
            "https://www.googleapis.com/drive/v3"

        private const val DRIVE_UPLOAD =
            "https://www.googleapis.com/upload/drive/v3"
    }
}

class DriveHttpException(
    val statusCode: Int,
    val responseBody: String,
) : RuntimeException(
    "Drive HTTP $statusCode",
)
