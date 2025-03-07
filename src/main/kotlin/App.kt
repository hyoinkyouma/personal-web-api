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
import kong.unirest.Unirest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.litote.kmongo.KMongo
import routes.BudgetAppRoutes
import services.TestServices

class App {

    companion object {
        val dotenv = try {dotenv()} catch (e:Throwable) {null}
        private  val Port:Int = System.getenv("PORT")?.toInt() ?: dotenv!!.get("PORT").toInt()
        lateinit var mongoDb:MongoDatabase
        lateinit var db: Database
        lateinit var mqttBroker: Broker

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
            val javalin: Javalin = Javalin.create().apply {
                this._conf.enableCorsForAllOrigins()
                this._conf.enableHttpAllowedMethodsOnRoutes()
                this._conf.enableDevLogging()

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
            }
        }





    @JvmStatic
    fun main(args: Array<String>) {
        initMongo()
        mongoDb.createCollection("budget")
        mongoDb.createCollection("budget-users")
        mongoDb.createCollection("budget-balance")
        mongoDb.createCollection("budget-user-settings")
        initJavalin()
        initializeUnirest()
        TestServices().generateKeyFile(null)
        initMqttBroker()
    }
    }
}

