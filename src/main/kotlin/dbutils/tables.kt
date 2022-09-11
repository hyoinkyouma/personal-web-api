package dbutils

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime
import java.util.UUID


object Requests: Table() {
    val payload = varchar("payload", 2056).nullable()
    val created_at = datetime("created_at")
    val id = varchar("id", 2056)
    val requestor_ip = varchar("requestor_ip", 255).nullable()
    val updated_at = datetime("updated_at")

    override val primaryKey = PrimaryKey(id, name = "PK_ID")
}

object Auth: Table() {
    val id = uuid("id").default(UUID.randomUUID())
    val userName = varchar("username", 255)
    val password = varchar("password", 255)
    val app_scope = varchar("app_scope", 255)

    override val primaryKey = PrimaryKey(id, name = "Auth_ID")
}