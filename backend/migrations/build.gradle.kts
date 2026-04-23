plugins {
    id("org.flywaydb.flyway") version "10.10.0"
}

repositories {
    mavenCentral()
}

val dbUrl     = System.getenv("FLYWAY_URL")     ?: "jdbc:postgresql://localhost:5432/agromis"
val dbUser    = System.getenv("FLYWAY_USER")    ?: "agromis"
val dbPwd     = System.getenv("FLYWAY_PASSWORD") ?: "agromis_dev_pass"
val appPwd    = System.getenv("APP_PASSWORD")    ?: "agromis_app_dev_pass"

flyway {
    url      = dbUrl
    user     = dbUser
    password = dbPwd
    locations = arrayOf("classpath:db/migration")
    placeholders = mapOf("app_password" to appPwd)
    validateOnMigrate = true
    outOfOrder = false
    baselineOnMigrate = false
    mixed = false
}

dependencies {
    // Driver JDBC
    runtimeOnly("org.postgresql:postgresql:42.7.3")
    // TimescaleDB no necesita driver adicional — usa el de PG
}

// Permite correr: ./gradlew :migrations:flywayMigrate
