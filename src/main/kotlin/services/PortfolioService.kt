package services

import App.Companion.mongoDb
import middlewares.Middleware
import io.javalin.http.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.litote.kmongo.findOne
import org.bson.Document
import org.json.JSONObject

class PortfolioService {
    private val mongoDB = mongoDb
    private val portfolioCoroutineHandler = CoroutineScope(Job())
    fun getPortfolio(it:Context):String {
        portfolioCoroutineHandler.launch{
            Middleware().requestLogging(it)
        }
        return try {
            JSONObject(
                mongoDB.getCollection("portfolio")
                    .findOne(Document(mapOf(
                        "item" to "portfolio"
                    )))).getJSONObject("value").toString()
        } catch (e:Throwable){
            e.printStackTrace()
            JSONObject(mapOf("Message" to "Request Failed")).toString()
        }
    }

    fun sendPortfolio(it:Context):String {

        val jsonBody = JSONObject(it.body())

        if (it.body().isNotEmpty()) {
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
                val sortedMap = docMap.toSortedMap()
                col.replaceOne(filterDocMap, Document(mapOf("item" to "portfolio", "value" to Document(sortedMap))))
            }
        }
        return JSONObject(mapOf("Message" to "Success")).toString()
    }

    fun removePorfolio(it:Context):String {
        val title = JSONObject(it.body()).optString("title")

        portfolioCoroutineHandler.launch {
            Middleware().requestLogging(it)
            val filter = Document(mapOf("item" to "portfolio"))
            val list = JSONObject(mongoDb.getCollection("portfolio")
                .findOne(filter))
                .optJSONObject("value")
                .toMap()
                .toMutableMap()
            list.remove(title)
            println(list)
            list.toSortedMap()
            mongoDb.getCollection("portfolio")
                .replaceOne(filter, Document(mapOf("item" to "portfolio", "value" to Document(list))) )
        }
        return JSONObject(mapOf("Status" to "Request Sent")).toString()
    }

}
