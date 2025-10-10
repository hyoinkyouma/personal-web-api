import routes.ServiceRoutes
import dbutils.DBConnection
import dbutils.Requests
import com.mongodb.client.MongoDatabase
import dbutils.Auth
import io.javalin.Javalin
import io.github.cdimascio.dotenv.dotenv
import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.broker.interfaces.PacketInterceptor
import io.github.davidepianca98.mqtt.packets.MQTTPacket
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTConnect
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPublish
import io.javalin.apibuilder.ApiBuilder
import io.javalin.core.util.Header
import io.javalin.core.util.FileUtil
import io.javalin.http.staticfiles.Location
import java.io.File
import kong.unirest.Unirest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.litote.kmongo.KMongo
import routes.BudgetAppRoutes
import services.TestServices

class App {

    companion object {
        val dotenv = try {dotenv()} catch (e:Throwable) {null}
        private val Port:Int = System.getenv("PORT")?.toInt() ?: dotenv!!.get("PORT").toInt()
        lateinit var mongoDb:MongoDatabase
        lateinit var db: Database
        lateinit var mqttBroker: Broker
        val okHttpClient = OkHttpClient()


        private fun initMongo () {
            mongoDb = KMongo.createClient(System.getenv("MongoString") ?: dotenv!!.get("MongoString")).getDatabase("personal")
        }

        private fun initDb () {
            println(System.getenv("PORT") ?: "None")
            DBConnection.init()
            this.db = Database.connect(DBConnection.db)
            transaction {
                SchemaUtils.create(Requests, Auth)
            }
        }

        private fun initMqttBroker() {
            mqttBroker = Broker(port = 1883,webSocketPort = 1884,enableUdp = true, host = "0.0.0.0", packetInterceptor = object : PacketInterceptor {
                override fun packetReceived(clientId: String, username: String?, password: UByteArray?, packet: MQTTPacket) {
                    when (packet) {
                        is MQTTConnect -> println(packet.protocolName)
                        is MQTTPublish -> {
                            if (packet.topicName == "test"){
                                println("Test Topic Payload:${packet.payload}")
                            }
                        }
                    }
                }
            }).also {
                it.listen()
            }
            println("MQTT Broker: SERVICE STARTED")

        }

        private fun initializeUnirest() {
            Unirest.config()
                .verifySsl(false)
                .socketTimeout(600000)
                .connectTimeout(600000)
                .concurrency(2000, 2000)
        }

        private fun initJavalin () {
            // Determine if we're running on Railway (production) or locally
            val isProduction = System.getenv("RAILWAY_ENVIRONMENT_NAME") != null
            println("Environment Prod: $isProduction")
            val staticFilesDir = if (isProduction) "/resources" else "images"
            val baseUrlReverseProxy = System.getenv("UIADDRESS") ?: dotenv!!.get("UIADDRESS")
            println("Reverse Proxy URL:$baseUrlReverseProxy")

            val javalin: Javalin = Javalin.create().apply {
                this._conf.enableCorsForAllOrigins()
                this._conf.enableHttpAllowedMethodsOnRoutes()
                this._conf.enableDevLogging()
                // Ensure directory exists in development mode

                val dir = File(staticFilesDir)
                if (!dir.exists()) {
                    dir.mkdir()
                    println("Created directory: ${dir.absolutePath}")
                }

                val portfolioImagesDir = File("$staticFilesDir/portfolio-images")
                if (!portfolioImagesDir.exists()) {
                    portfolioImagesDir.mkdir()
                    println("Created directory: ${portfolioImagesDir.absolutePath}")
                }
                // Configure static files handling
                // The external location flag ensures files are loaded from the file system
                // addStaticFiles handles the directory as the web root
                this._conf.addStaticFiles { staticFiles ->
                    staticFiles.directory = staticFilesDir
                    staticFiles.location = Location.EXTERNAL
                    // Enable hot-reloading (no need to restart server when files change)
                    staticFiles.hostedPath = "/"
                    staticFiles.precompress = false
                }
                
                // Also serve portfolio-images at a specific URL path
                this._conf.addStaticFiles { staticFiles ->
                    staticFiles.directory = "$staticFilesDir/portfolio-images"
                    staticFiles.location = Location.EXTERNAL
                    staticFiles.hostedPath = "/portfolio-images"
                    staticFiles.precompress = false
                }
                
                println("Static files configured from: $staticFilesDir")
                if (isProduction) {
                    println("Running in production mode on Railway")
                } else {
                    println("Running in development mode locally")
                    

                }
            }.start(Port)
            
            javalin.routes {

                ApiBuilder.path("/v1") {
                    ApiBuilder.before {
                        it.header(Header.ACCESS_CONTROL_ALLOW_HEADERS, "Access-Control-Allow-Headers, Authorization, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers")
                        
                        /*if (!it.basicAuthCredentialsExist()) {
                              it.header("WWW-Authenticate", "Basic realm=\"User Visible Realm\", charset=\"UTF-8\"")
                              throw HttpResponseException(401, "Login required")
                          }*/
                        it.header(Header.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        it.header(Header.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                        it.header(Header.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST")
                    }
                    ServiceRoutes().start()
                    ApiBuilder.path("/financial"){
                        BudgetAppRoutes().start()
                    }
                }
                ApiBuilder.get("/admin-page-website") {
                    it.redirect(baseUrlReverseProxy + it.path())
                }
                ApiBuilder.get("/*") { ctx ->
                    val query = ctx.queryString()?.let {"?$it" } ?: ""
                    val targetUrl = "$baseUrlReverseProxy${ctx.path()}$query"
                    println(targetUrl)

                    val request = Request.Builder()
                        .url(targetUrl)
                        .get()
                        .build()

                    try {
                        okHttpClient.newCall(request).execute().use { response ->
                            ctx.status(response.code)

                            // Set the content type if present
                            val contentType = response.header("Content-Type") ?: "text/html"
                            ctx.header("Content-Type", contentType)

                            response.body?.bytes()?.let { 
                                ctx.result(it)
                            }
                        }
                    } catch (e: Exception) {
                        ctx.status(502).result("Proxy error: ${e.message}")
                    }
                }
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            initMongo()
//            mongoDb.createCollection("budget")
//            mongoDb.createCollection("budget-users")
//            mongoDb.createCollection("budget-balance")
//            mongoDb.createCollection("budget-user-settings")
            initJavalin()
            initializeUnirest()
            TestServices().generateKeyFile(null)
            initMqttBroker()
        }
    }
}

