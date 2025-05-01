package services

import App.Companion.mongoDb
import io.javalin.http.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import middlewares.Middleware
import org.bson.Document
import org.json.JSONObject
import org.litote.kmongo.findOne
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID



class PortfolioService {
    private val mongoDB = mongoDb
    private val portfolioCoroutineHandler = CoroutineScope(Job())
    fun getPortfolio(it: Context): String {
        portfolioCoroutineHandler.launch {
            Middleware().requestLogging(it)
        }
        return try {
            JSONObject(
                mongoDB.getCollection("portfolio")
                    .findOne(
                        Document(
                            mapOf(
                                "item" to "portfolio"
                            )
                        )
                    )
            ).getJSONObject("value").toString()
        } catch (e: Throwable) {
            e.printStackTrace()
            JSONObject(mapOf("Message" to "Request Failed")).toString()
        }
    }

    fun login(it: Context): String {
        println(
            it.body()
        )
        val jsonBody = JSONObject(it.body())
        val response = JSONObject()
        try {
            val auth = JSONObject(mongoDB.getCollection("auth").findOne(Document(mapOf("item" to "portfolio"))))
            val username = auth.getString("username")
            val password = auth.getString("password")

            if (username == jsonBody.getString("username")) {
                response.put("username", "valid")
                if (password == jsonBody.getString("password")) {
                    response.put("password", "valid")
                    response.put("key", Middleware().sessionKeysEncrypt(username, password))
                } else {
                    response.put("password", "invalid")
                }
            } else {
                response.put("username", "invalid")
                response.put("password", "invalid")
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return response.toString()
    }

    fun verifyLoggedIn(it: Context): String {
        val jsonBody = JSONObject(it.body())
        val response = JSONObject()
        if (verify(jsonBody.getString("key"))) {
            response.put("valid", true)
        } else response.put("valid", false)
        return response.toString()
    }

    private fun verify(sessionKey: String): Boolean = Middleware().sessionKeysValidate(sessionKey)
    fun sendPortfolio(it: Context): String {

        val jsonBody = JSONObject(it.body())

        if (it.body().isNotEmpty() && verify(jsonBody.getString("key"))) {
            val col = mongoDB.getCollection("portfolio")

            portfolioCoroutineHandler.launch {
                Middleware().requestLogging(it)
                val filterDocMap = Document(mapOf("item" to "portfolio"))
                val docMap = JSONObject(col.findOne(filterDocMap))
                    .getJSONObject("value")
                    .toMap().toMutableMap()

                docMap[jsonBody.optString("title")] = Document(
                    mutableMapOf(
                        "link" to jsonBody.optString("link"),
                        "img" to jsonBody.optString("img"),
                        "desc" to jsonBody.optString("desc")
                    )
                )
                col.replaceOne(filterDocMap, Document(mapOf("item" to "portfolio", "value" to Document(docMap))))
            }
            return JSONObject(mapOf("Message" to "Success")).toString()
        } else {
            return JSONObject(mapOf("Message" to "Failed")).toString()
        }
    }

    fun removePorfolio(it: Context): String {
        val title = JSONObject(it.body()).optString("title")
        if (verify(JSONObject(it.body()).optString("key"))) {
            Middleware().requestLogging(it)
            val filter = Document(mapOf("item" to "portfolio"))
            val list = JSONObject(
                mongoDb.getCollection("portfolio")
                    .findOne(filter)
            )
                .optJSONObject("value")
                .toMap()
                .toMutableMap()
            return if (list.containsKey(title)) {
                list.remove(title)
                list.toSortedMap()
                mongoDb.getCollection("portfolio")
                    .replaceOne(filter, Document(mapOf("item" to "portfolio", "value" to Document(list))))
                JSONObject(mapOf("Status" to "Request Sent")).toString()

            } else {
                JSONObject(mapOf("Status" to "404")).toString()
            }
        }
        return JSONObject(mapOf("Status" to "Failed")).toString()
    }

    fun uploadProjectImages(it: Context): String {
        val response = JSONObject()
        
        try {
            // Determine if we're running on Railway (production) or locally
            val isProduction = System.getenv("RAILWAY_ENVIRONMENT_NAME") != null
            val rootDir = if (isProduction) "/resources" else "images"
            val baseDir = "$rootDir/portfolio-images"
            
            // Create directory if it doesn't exist
            val imageDir = File(baseDir)
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }
            
            // Get uploaded file
            val uploadedFile = it.uploadedFile("image")
            
            if (uploadedFile != null) {
                val filename = uploadedFile.filename
                val filePath = Paths.get(baseDir, filename)
                
                // Delete existing file if it exists
                val file = filePath.toFile()
                if (file.exists()) {
                    file.delete()
                }
                
                // Save the file
                Files.copy(uploadedFile.content, filePath)
                
                // Construct the image URL using RAILWAY_PUBLIC_DOMAIN if in production
                val host = if (isProduction) {
                    System.getenv("RAILWAY_PUBLIC_DOMAIN") ?: it.host()
                } else {
                    it.host() ?: "localhost:${System.getenv("PORT") ?: "8080"}"
                }
                val scheme = if (isProduction) "https" else "http"
                val imageUrl = "$scheme://$host/portfolio-images/$filename"
                
                // Return success with the file path and direct link
                response.put("success", true)
                response.put("filename", filename)
                response.put("path", "/portfolio-images/$filename")
                response.put("link", imageUrl)
                response.put("message", "Image uploaded successfully")
            } else {
                response.put("success", false)
                response.put("message", "No file uploaded")
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            response.put("success", false)
            response.put("message", "Upload failed: ${e.message}")
        }
        
        return response.toString()
    }

}
