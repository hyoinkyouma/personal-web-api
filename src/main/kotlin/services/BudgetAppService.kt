package services

import App.Companion.mongoDb
import com.mongodb.MongoException
import de.undercouch.bson4jackson.types.Decimal128
import org.bson.Document
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import org.json.JSONArray
import org.json.JSONObject
import org.litote.kmongo.findOne
import java.math.BigDecimal
import java.util.UUID
import java.util.UUID.randomUUID
import javax.print.Doc

class BudgetAppService {
    private val usersCollection = mongoDb.getCollection("budget-users")
    private val budgetTransactionCollection = mongoDb.getCollection("budget")
    private val budgetBalanceCollection = mongoDb.getCollection("budget-balance")
    private val budgetUserSettingsCollection = mongoDb.getCollection("budget-user-settings")
    private data class LoginInfo (val username:String, val password:String)

    fun deleteTransaction(userKey: String, uuid:String):JSONObject {
        val response = JSONObject()
        response.put("user-key", userKey)
        
        return try {
            val result = budgetTransactionCollection.deleteOne(Document(mapOf("user-key" to userKey, "id" to uuid)))
            if (result.wasAcknowledged()) {
                response.put("success", true)
            } else{
                response.put("success", false)
            }
            response
        } catch (e:Throwable) {
            response.put("success", false)
            response
        }
    }

    fun getTransactions(userKey: String):JSONObject? {
        val response = JSONObject()
        response.put("user-key", userKey)
        return try {
            val transactionArray = JSONArray()
            val transactions = budgetTransactionCollection.find(Document(mapOf("user-key" to userKey)))
            transactions.forEach {
                transactionArray.put(
                    JSONObject(
                        mapOf<String, String>(
                            "description" to it["description"].toString(),
                            "amount" to it["amount"].toString(),
                            "type" to it["type"].toString(),
                            "date-created" to it["date-created"].toString(),
                            "user-key" to it["user-key"].toString(),
                            "_id" to it["_id"].toString()

                        )
                    ))
            }
            response.put("code", 200)
            response.put("success", true)
            response.put("transactions", transactionArray)
            response
        } catch (_:Throwable) {
            response.put("code", 500)
            response.put("success", false)
            response
        }
    }

    fun getBalance(userKey: String):JSONObject? {
        return try{
            budgetBalanceCollection.findOne { Document(mapOf("user-key" to userKey)) }.let {
                val balance = it?.get("balance") ?: BigDecimal.ZERO
                val newUserKey = it?.get("user-key")?: ""
                val lastUpdated = it?.get("last-updated") ?: ""
                return@let JSONObject(mapOf<String, String>("balance" to balance.toString(), "user-key" to newUserKey.toString(), "last-updated" to lastUpdated.toString()))
            }
        } catch (e:Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun deductBalance (userKey:String, amount: BigDecimal):JSONObject? {
        val balanceDoc = budgetBalanceCollection.findOne { Document(mapOf("user-key" to userKey)) }
        if (balanceDoc.isNullOrEmpty()) {
            return JSONObject(budgetBalanceCollection.insertOne(
                Document(
                    mapOf(
                        "balance" to amount.toString(),
                        "user-key" to userKey,
                        "date-created" to DateTime.now(DateTimeZone.forID("Asia/Manila")).toString()
                    )
                )
            ))
        } else  {
            val currentBalance = balanceDoc["balance"].toString().toBigDecimalOrNull()
            return if (currentBalance == null) {
                null
            } else {
                budgetBalanceCollection.deleteOne(balanceDoc)
                val newBalance = currentBalance - amount
                JSONObject(budgetBalanceCollection.insertOne(Document(mapOf(
                    "balance" to newBalance.toString(),
                    "user-key" to userKey,
                    "date-created" to DateTime.now(DateTimeZone.forID("Asia/Manila")).toString()
                ))))
            }
    }
    }
    private fun addBalance (userKey:String, amount:BigDecimal):JSONObject? {
        val balanceDoc = budgetBalanceCollection.findOne { Document(mapOf("user-key" to userKey)) }
        if (balanceDoc.isNullOrEmpty()) {
            return JSONObject(budgetBalanceCollection.insertOne(Document(mapOf(
                "balance" to amount.toString(),
                "user-key" to userKey,
                "date-created" to DateTime.now(DateTimeZone.forID("Asia/Manila"))
            ))))
        } else {
            val currentBalance = balanceDoc["balance"].toString().toBigDecimalOrNull()
            return if (currentBalance == null) {
                null
            } else {
                budgetBalanceCollection.deleteOne(balanceDoc)
                val newBalance = currentBalance + amount
                JSONObject(budgetBalanceCollection.insertOne(Document(mapOf(
                    "balance" to newBalance.toString(),
                    "user-key" to userKey,
                    "date-created" to DateTime.now(DateTimeZone.forID("Asia/Manila"))
                ))))
            }
        }
    }
    fun createTransaction(transactionString:String):JSONObject{
        val response = JSONObject()
        val transactionJSON = JSONObject(transactionString)
        val type = transactionJSON.optString("type")
        val amount = transactionJSON.getBigDecimal("amount")
        val userKey = transactionJSON.getString("user-key")
        val dateCreated = transactionJSON.optString("date-created").ifBlank { DateTime.now(DateTimeZone.forID("Asia/Manila")) }
        val description = transactionJSON.optString("description")

        if (type.lowercase() == "deposit") {
            val insertTransactionResult = budgetTransactionCollection.insertOne(
                Document(mapOf(
                    "id" to randomUUID(),
                    "type" to type,
                    "amount" to amount,
                    "user-key" to userKey,
                    "date-created" to dateCreated,
                    "description" to description
                ))
            )
            if (insertTransactionResult.wasAcknowledged()) {
                addBalance(userKey, amount)
                response.put("success", true)
                response.put("code", "200")
            } else {
                response.put("success", false)
                response.put("code", "500")
            }

        } else {
            val insertTransactionResult = budgetTransactionCollection.insertOne(
                Document(mapOf(
                    "id" to UUID.randomUUID().toString(),
                    "type" to type,
                    "amount" to amount,
                    "user-key" to userKey,
                    "date-created" to dateCreated.toString(),
                    "description" to description
                ))
            )
            if (insertTransactionResult.wasAcknowledged()) {
                deductBalance(userKey, amount)
                response.put("success", true)
                response.put("code", "200")
            } else {
                response.put("success", false)
                response.put("code", "500")
            }
        }
        return response
    }

    fun signUp (signUpInfoString: String):JSONObject {
        val signUpInfoStringJSON = JSONObject(signUpInfoString)
        val email = signUpInfoStringJSON.getString("email")

        val response = JSONObject()
        usersCollection.findOne { Document().append("email", email) }.let {
            if (it == null) {
                val password = signUpInfoStringJSON.getString("password")
                val fName = signUpInfoStringJSON.getString("fname")
                val sName = signUpInfoStringJSON.getString("sname")
                val key = UUID.randomUUID().toString()
                usersCollection.insertOne(
                    Document(mapOf<String, Any>("password" to password, "email" to email, "fName" to fName, "sName" to sName, "key" to key))
                )
                response.put("code", 200)
                response.put("success", true)
                response.put("key", key)
            } else {
                response.put("code", 200)
                response.put("success", false)
                response.put("msg", "Email Already Exists")
            }
        }
        return response
    }

    private fun loginWithKey (key:String):JSONObject? = try {
            JSONObject(usersCollection.findOne{Document(mapOf<String, String>("key" to key))})
        } catch (_:Throwable){
            null
        }

    fun getUser (userKey:String):JSONObject? = try { JSONObject(usersCollection.findOne{Document(mapOf<String, String>("key" to userKey))}) } catch (e:Throwable) { e.printStackTrace(); null }

    fun login (loginInfoString:String):JSONObject {
        val response = JSONObject()
        val loginInfoStringJSON = JSONObject(loginInfoString)
        if (loginInfoStringJSON.optBoolean("user-key", false)) {
            val userdata = loginWithKey(loginInfoStringJSON.getString("key")) ?: JSONObject()
            if (!userdata.isEmpty) {
                response.put("success", true)
                response.put("user", userdata)
                response.put("code", "200")
                return response
            }
            response.put("success", false)
            response.put("code", "403")
            return response
        }
        if (loginInfoStringJSON.getString("email").isNullOrBlank() || loginInfoStringJSON.getString("password").isNullOrBlank()) {
            response.put("success", false)
            response.put("code", "403")
            response.put("msg", "Email or password is empty")
        }

        try {
            val loginInfo = LoginInfo(
                username = loginInfoStringJSON.getString("email"),
                password = loginInfoStringJSON.getString("password")
            )
            usersCollection.findOne(Document(mapOf<String,String>("email" to loginInfo.username)))?.let {
                if (it.isNotEmpty()) {
                   if (it["password"] == loginInfo.password) {
                       response.put("success", true)
                       response.put("user", JSONObject(it))
                       response.put("code", "200")
                   } else {
                       response.put("success", false)
                       response.put("msg", "Incorrect Password")
                       response.put("code", "403")
                   }
                } else {
                    response.put("success", false)
                    response.put("msg", "User Not Found")
                    response.put("code", "403")
                }
            } ?: run {
                response.put("success", false)
                response.put("msg", "User Not Found")
                response.put("code", "403")
            }
        } catch (e:Throwable) {
            response.put("success", false)
            response.put("msg", "Internal Service Error")
            response.put("code", "500")
            println(e)
        }
        return response
    }

    fun setBalance(request: JSONObject): Any {
        val userKey = request.getString("user-key")
        val balance = request.getBigDecimal("balance")
        val response = JSONObject()

        try {
            val existing = budgetBalanceCollection.findOne(Document("user-key", userKey))

            if (existing.isNullOrEmpty()) {
                budgetBalanceCollection.insertOne(
                    Document(mapOf(
                        "user-key" to userKey,
                        "balance" to balance,
                        "last-updated" to DateTime.now().toString()
                    ))
                )
            } else {
                budgetBalanceCollection.updateOne(
                    Document("user-key", userKey),
                    Document("\$set", mapOf(
                        "balance" to balance,
                        "last-updated" to DateTime.now().toString()
                    ))
                )
            }

            val balanceDoc = budgetBalanceCollection.findOne(Document("user-key", userKey))
            response.put("balance", balanceDoc?.get("balance"))
            response.put("user-key", userKey)
            response.put("success", true)

        } catch (e: MongoException) {
            e.printStackTrace()
            response.put("success", false)
            response.put("msg", "failed to update balance")
        }

        return response
    }


}