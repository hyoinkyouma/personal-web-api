package middlewares

import io.javalin.http.Context
import org.bson.Document
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONObject
import org.litote.kmongo.findOne
import org.litote.kmongo.save

class Middleware {
    private val mongoDb = App.mongoDb
    fun verify (it:Context):Boolean {
        try {
            val authUser = it.basicAuthCredentials()
            val authVars = mongoDb.getCollection("auth")
                .findOne { Document(mapOf("item" to "portfolio")) }

            if (authUser.username == authVars?.getString("username") &&
                authUser.password == authVars.getString("password")
            ) {
                return true
            }
            return false
        } catch (e:Throwable) {
            return false
        }
    }
    fun requestLogging (it: Context) {
            val col = mongoDb.getCollection("request_logs")
            val currentDateTime = DateTime.now(DateTimeZone.forID("Asia/Manila"))
            val bodyMap = JSONObject(
                if (it.body().isBlank()) {
                    it.body()
                } else mapOf("items" to "none")
            ).toMap()

            val request = Document(
                mapOf(
                    "endpoint" to it.fullUrl(),
                    "time" to currentDateTime.toString(),
                    "body" to Document(bodyMap),
                    "ip" to it.ip()
                )
            )
            col.save(request)
    }
}