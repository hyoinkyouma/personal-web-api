package Middlewares

import dbutils.Auth
import io.javalin.http.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import App.Companion.mongoDb
import org.bson.Document
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONObject
import org.litote.kmongo.save
import java.math.BigInteger

class Middleware {
    private val MongoDb = mongoDb
    fun verify (it:Context):Boolean {
        try {
            val authUser = it.basicAuthCredentials()
            val authVars = transaction {Auth.slice(Auth.userName, Auth.password)
                .select { Auth.app_scope eq "portfolio"}.first()}
            if (authUser.username == authVars[Auth.userName] && authUser.password == authVars[Auth.password]) {
                return true
            }
            return false
        } catch (e:Throwable) {
            return false
        }
    }
    suspend fun requestLogging (it: Context, endpoint:String = "") {
            val col = MongoDb.getCollection("request_logs")
            val currentDateTime = DateTime.now(DateTimeZone.forID("Asia/Manila"))
            val bodyMap = JSONObject(it.body()).toMap()
            val request = Document(
                mapOf(
                    "endpoint" to endpoint,
                    "time" to currentDateTime.toString(),
                    "body" to Document(bodyMap),
                    "ip" to it.ip()
                )
            )
            col.save(request)
    }
}