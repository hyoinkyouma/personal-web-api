package routes

import io.javalin.apibuilder.ApiBuilder
import org.json.JSONObject
import services.BudgetAppService

class BudgetAppRoutes {
    private val budgetAppService = BudgetAppService()
    fun start () {
        ApiBuilder.post("/login"){
            val response = budgetAppService.login(it.body())
            val responseCode = response.getInt("code")
            it.result(response.toString())
            it.status(responseCode)
        }
        ApiBuilder.post("/sign-up") {
            val response = budgetAppService.signUp(it.body())
            val responseCode = response.getInt("code")
            it.result(response.toString())
            it.status(responseCode)
        }
        ApiBuilder.post("/create-transaction") {
            val response = budgetAppService.createTransaction(it.body())
            val responseCode = response.getInt("code")
            it.result(response.toString())
            it.status(responseCode)
        }
        ApiBuilder.post("/get-balance") {
            val response = budgetAppService.getBalance(JSONObject(it.body()).getString("user-key"))
            if (response != null) {
                it.result(JSONObject(mapOf("balance" to response, "success" to true)).toString())
                it.status(200)
            } else {
                it.result(JSONObject(mapOf("success" to false)).toString())
                it.status(403)
            }
        }
        ApiBuilder.post("/get-transactions") {
                it.result(budgetAppService.getTransactions(
                    JSONObject(
                        it.body())
                        .getString("user-key"))
                        .toString()
                )
        }
    }
}