package middlewares

import App
import io.javalin.http.Context
import org.bson.Document
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json.JSONObject
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import java.io.File
import java.nio.file.Files
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPrivateCrtKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import kotlin.math.log


class Middleware {
    private val mongoDb = App.mongoDb

    fun sessionKeysValidate(sessionKey:String):Boolean {
        return try {
            val encrypCipher = Cipher.getInstance("RSA")
            val privateKey = getPrivateKey()
            encrypCipher.init(Cipher.DECRYPT_MODE, privateKey)
            val sessionKeyDecoded = Base64.getDecoder().decode(sessionKey.toByteArray(Charsets.UTF_8))
            val jsonSession = JSONObject(encrypCipher.doFinal(sessionKeyDecoded).toString(Charsets.UTF_8))
            println(jsonSession.toString())
            val loginCredentials = JSONObject(mongoDb.getCollection("auth").findOne("{username:\"${jsonSession.getString("username")}\"}")?.toMap())
            println(loginCredentials.toString())
            val isValidUsername = loginCredentials.getString("username") == jsonSession.getString("username")
            val isValidPassword = loginCredentials.getString("password") == jsonSession.getString("password")
            isValidPassword && isValidUsername
        } catch (e:Throwable) {
            false
        }
    }

    fun sessionKeysEncrypt(username:String, password:String):String {
        val jsonSession = JSONObject(
            mapOf(
                "username" to username,
                "password" to password
            )
        ).toString()
        val encryptCipher = Cipher.getInstance("RSA")
        encryptCipher.init(Cipher.ENCRYPT_MODE, getPubKey())
        val sessionByteArray = jsonSession.toByteArray(Charsets.UTF_8)
        val sessionKey = Base64.getEncoder().encodeToString(encryptCipher.doFinal(sessionByteArray))
        return sessionKey
    }

    private fun getPubKey():PublicKey {
        val publicKeyFile = File("public.key")
        val publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath())
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(publicKeyBytes)
        return keyFactory.generatePublic(publicKeySpec)
    }
    private fun getPrivateKey():PrivateKey {
        val privateKeyFile = File("private.key")
        val privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath())
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        return  keyFactory.generatePrivate(privateKeySpec)
    }

    fun requestLogging (it: Context) {
            val col = mongoDb.getCollection("request_logs")
            val currentDateTime = DateTime.now(DateTimeZone.forID("Asia/Manila"))
            val bodyMap = JSONObject(
                if (it.body().isBlank()) {
                    it.body()
                } else mapOf("items" to "none")
            ).toMap()

            val request = Document(
                mapOf(
                    "endpoint" to it.fullUrl(),
                    "time" to currentDateTime.toString(),
                    "body" to Document(bodyMap),
                    "ip" to it.ip()
                )
            )
            col.save(request)
    }
}