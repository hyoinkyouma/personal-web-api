package Services

import App.Companion.mongoDb
import Middlewares.Middleware
import io.javalin.http.Context
import kong.unirest.Unirest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.litote.kmongo.findOne
import org.bson.Document
import org.json.JSONObject
import org.slf4j.spi.SLF4JServiceProvider

class PortfolioService {
    private val MongoDB = mongoDb
    private val portfolioCoroutineHandler = CoroutineScope(Job())
    fun getPortfolio(it:Context):String {
        portfolioCoroutineHandler.launch{
            Middleware().requestLogging(it)
        }
        return try {
            JSONObject(
                MongoDB.getCollection("portfolio")
                    .findOne(Document(mapOf(
                        "item" to "portfolio"
                    )))).getJSONObject("value").toString()
        } catch (e:Throwable){
            e.printStackTrace()
            JSONObject(mapOf("Message" to "Request Failed")).toString()
        }
    }

    fun sendPortfolio(it:Context):String {
        portfolioCoroutineHandler.launch {
            Middleware().requestLogging(it)
        }
        val jsonBody = JSONObject(it.body())
        var isValid = false
        try {
        for (it in jsonBody.toMap()){
            if (it.key != "desc") {
                isValid = checkUrl(it.value as String).isSuccess
            }
            if (!isValid) break
        }} catch (e:Throwable) {
            isValid = false
            e.printStackTrace()
        }

        if (it.body().isNotEmpty() && isValid) {
            val col = MongoDB.getCollection("portfolio")

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
        return JSONObject(mapOf("Message" to if (isValid) "Success" else "Failed Get URL Resources")).toString()
    }

    private fun checkUrl (url:String) = Unirest.get(url).asString()
}
