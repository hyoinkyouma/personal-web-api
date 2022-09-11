import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    application
}

group = "tk.roman.web"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    testImplementation(kotlin("test"))

    //datetime modern workaround
    implementation ("org.jetbrains.exposed:exposed-jodatime:0.39.2")

    //Postgres
    implementation ("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
    implementation ("org.jetbrains.exposed:exposed-jdbc:0.39.2")
    implementation ("com.zaxxer:HikariCP:5.0.1")

    //unirest
    implementation("com.konghq:unirest-java:3.11.09")

    //javalin
    implementation("io.javalin:javalin:4.6.4")
    implementation("org.slf4j:slf4j-simple:2.0.0")

    //JSON
    implementation("org.json:json:20220320")

    //mongo
    implementation("org.litote.kmongo:kmongo:4.7.0")

    //env
    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    // Define the main class for the application.
    mainClass.set("AppKt")
    mainClassName = "App"
}