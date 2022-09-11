import Routes.ServiceRoutes
import dbutils.DBConnection
import dbutils.Requests
import com.mongodb.client.MongoDatabase
import dbutils.Auth
import io.javalin.Javalin
import io.github.cdimascio.dotenv.dotenv
import io.javalin.apibuilder.ApiBuilder
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.litote.kmongo.KMongo

class App {

    companion object {
        val dotenv = dotenv()
        private  val Port:Int = dotenv.get("PORT").toInt()
        lateinit var mongoDb:MongoDatabase
        lateinit var db: Database

        private fun initMongo () {
            mongoDb = KMongo.createClient(dotenv.get("MongoString")).getDatabase("personal")
        }

        private fun initDb () {
            DBConnection.init()
            this.db = Database.connect(DBConnection.db)
            transaction {
                SchemaUtils.create(Requests, Auth)
            }
        }

        private fun initJavalin () {
            val javalin: Javalin = Javalin.create { config ->
                config.maxRequestSize = 122880L
                config.enableCorsForAllOrigins()
            }.start(Port)
            javalin.routes {
                ApiBuilder.path("/v1") {
                    ServiceRoutes().start()

                }
            }
        }





    @JvmStatic fun main(args: Array<String>) {
        initDb()
        initMongo()
        initJavalin()

    }
    }
}

