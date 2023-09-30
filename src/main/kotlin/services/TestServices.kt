package services

import App.Companion.mongoDb
import dbutils.Requests
import io.javalin.http.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bson.Document
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONObject
import org.litote.kmongo.findOne
import org.litote.kmongo.json
import java.io.FileOutputStream
import java.security.KeyPairGenerator
import java.util.*


class TestServices {
    private val dbCoroutine = CoroutineScope(Job())

    fun generateKeyFile(it:Context?):String {
        val jsonResponse = JSONObject()

        try {
            val generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(2048)
            val pair = generator.generateKeyPair()

            val pubkey = pair.public
            val pk = pair.private

            FileOutputStream("public.key").use { fos -> fos.write(pubkey.encoded) }
            FileOutputStream("private.key").use { fos -> fos.write(pk.encoded) }

            jsonResponse.put("result_success", true)
        } catch (e:Throwable) {
            e.printStackTrace()
            jsonResponse.put("result_success", false)
            jsonResponse.put("error_type", e.stackTraceToString())
        }
        return jsonResponse.toString()
    }
    fun test (it:Context):String {
        val jsonBody = JSONObject(it.body())
        println(jsonBody)
        dbCoroutine.launch {
            try {
                val id = UUID.randomUUID().toString()
                val label = if (jsonBody.optString("label").isNotEmpty()) jsonBody.getString("label") else "None"
                val date =  DateTime.now(DateTimeZone.forID("Asia/Manila"))

                val collection = mongoDb.getCollection("test")
                val document = Document()
                document["payload"] = jsonBody.toMap()
                val exists = collection.findOne(document)
                if (exists == null) {
                    document["label"] = label
                    document["id"] = id
                    document["date"] = date.toString()
                    document["requestor_ip"] = it.ip()
                    collection.insertOne(document)
                }
                transaction {
                    Requests.insert { x ->
                        x[this.id] = id
                        x[this.payload] = jsonBody.toString()
                        x[this.requestor_ip] = it.ip()
                        x[this.created_at] = date
                        x[this.updated_at] = date
                    }
                }
            }
            catch (e:Throwable) {
                e.printStackTrace()
            }
        }

        val res = JSONObject()
        res.put("MSG", "Success")
        return res.toString()
    }
}