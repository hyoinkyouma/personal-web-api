package routes

import middlewares.Middleware
import services.PortfolioService
import services.TestServices
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.Context
import org.json.JSONObject

class ServiceRoutes {
    private val portfolioService = PortfolioService()

    fun start () {
        ApiBuilder.post("/test"){
            it.result(TestServices().test(it))
        }

        ApiBuilder.get("/getPortfolio") {
            it.result(portfolioService.getPortfolio(it))
        }

        ApiBuilder.post("/sendPortfolio") {
            it.result(portfolioService.sendPortfolio(it))
        }

        ApiBuilder.post("/removePortfolio") {
            it.result(portfolioService.removePorfolio(it))
        }

    }
}