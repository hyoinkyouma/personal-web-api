package Services

import App.Companion.mongoDb
import Middlewares.Middleware
import io.javalin.http.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.litote.kmongo.findOne
import org.bson.Document
import org.json.JSONObject

class PortfolioService {
    private val MongoDb = mongoDb
    private val portfolioCoroutineHandler = CoroutineScope(Job())
    fun getPortfolio(it:Context):String {
        portfolioCoroutineHandler.launch{
            Middleware().requestLogging(it, "/getPortfolio")
        }
        try {
        return MongoDb.getCollection("portfolio")
            .findOne(Document(mapOf(
            "item" to "portfolio"
        ))).toString()
        } catch (e:Throwable){
            e.printStackTrace()
            return JSONObject(mapOf("Message" to "Request Failed")).toString()
        }
    }

    fun sendPortfolio(it:Context):String {
        portfolioCoroutineHandler.launch {
            Middleware().requestLogging(it, "/sendPortfolio")
        }



        if (it.body().isNotEmpty()) {
            val col = MongoDb.getCollection("portfolio")
            val jsonBody = JSONObject(it.body())

            portfolioCoroutineHandler.launch {
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
}
