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
        ApiBuilder.post("/get-user") {
            it.result(budgetAppService.getUser(JSONObject(it.body()).optString("user-key")).toString())
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
        ApiBuilder.get("/get-balance") {
            val response = budgetAppService.getBalance(it.queryParam("user-key") ?: "")
            if (response != null) {
                it.result(response.toString())
                it.status(200)
            } else {
                it.status(404)
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
        ApiBuilder.post("/set-balance") {
            it.result(budgetAppService.setBalance(JSONObject(it.body())).toString())
        }
        ApiBuilder.post("/delete-transaction") {
            it.result(
                budgetAppService.deleteTransaction(
                    JSONObject(it.body()).getString("user-key"),
                    JSONObject(it.body()).getString("id")
                ).toString()
            )
        }
    }
}