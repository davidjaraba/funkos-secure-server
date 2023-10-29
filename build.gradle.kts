plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.projectreactor:reactor-core:3.5.10")
    implementation("com.h2database:h2:2.1.210")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.mybatis:mybatis:3.5.13")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("io.r2dbc:r2dbc-h2:1.0.0.RELEASE")
    implementation("io.r2dbc:r2dbc-pool:1.0.0.RELEASE")

    // JWT
    implementation("com.auth0:java-jwt:4.2.1")
    // BCcrypt
    implementation("org.mindrot:jbcrypt:0.4")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "dev/server/Server"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}