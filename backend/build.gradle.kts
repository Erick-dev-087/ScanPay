plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "com.scanpay"
version = "1.0.0"
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.scanpay.server.ApplicationKt")
}

tasks.register<JavaExec>("checkAstra") {
    group = "verification"
    description = "Verify the configured Astra CQL connection without changing schema"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.scanpay.server.db.AstraConnectionCheckKt")
}

val ktorVersion = "2.3.12"

dependencies {
    implementation("org.apache.cassandra:java-driver-core:4.18.1")
    // Ktor Server Core & Netty
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    // Kotlinx Serialization & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // HTTP Client for Astra DB & Daraja
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Security & Hashing
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.auth0:java-jwt:4.4.0")

    // QR Code Generation
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

