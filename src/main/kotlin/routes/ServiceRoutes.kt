package routes

import middlewares.Middleware
import services.PortfolioService
import services.TestServices
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.Context
import org.json.JSONObject

class ServiceRoutes {
    private val m = Middleware()
    private val portfolioService = PortfolioService()

    fun start () {
        ApiBuilder.post("/test"){
            it.result(TestServices().test(it))
        }

        ApiBuilder.get("/getPortfolio") {
            val isValid = m.verify(it)
            if (isValid) it.result(portfolioService.getPortfolio(it))
            else respondFailed(it)
        }

        ApiBuilder.post("/sendPortfolio") {
            val isValid =  m.verify(it)
            if (isValid) it.result(portfolioService.sendPortfolio(it))
            else respondFailed(it)
        }

        ApiBuilder.post("/removePortfolio") {
            val isValid = m.verify(it)
            if (isValid) it.result(portfolioService.removePorfolio(it))
            else respondFailed(it)
        }

    }

    private fun respondFailed(it:Context) {
        it.result(JSONObject(mapOf("Error" to "You will not pass")).toString())
        it.status(403)
    }


}