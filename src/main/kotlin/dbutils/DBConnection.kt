package dbutils

import App.Companion.dotenv
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object DBConnection {
    lateinit var db: HikariDataSource

    fun init() {
        val config = HikariConfig()
        config.jdbcUrl =
            "jdbc:pgsql://${System.getenv("DATABASE_URL") ?: dotenv!!.get("DATABASE_URL")}?sslMode=Require"
        config.connectionTimeout = 30_000
        config.maxLifetime = 1_800_000
        config.idleTimeout = 600_000
        config.username = System.getenv("NODE_DB_USERNAME") ?: dotenv!!.get("NODE_DB_USERNAME").toString()
        config.password = System.getenv("NODE_DB_PASSWORD") ?: dotenv!!.get("NODE_DB_PASSWORD").toString()

        db = HikariDataSource(config)
    }
}