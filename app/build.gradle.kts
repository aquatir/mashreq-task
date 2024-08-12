// TODO: move artifacts and versions to gradle/libs.versions.toml file
plugins {
    id("buildlogic.kotlin-application-conventions")

    // Spring Plugins
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("plugin.spring") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.noarg") version "2.0.0"

    id("com.revolut.jooq-docker") version "0.3.12"
}

allOpen {
    annotations(
        "org.springframework.stereotype.RestController",
        "org.springframework.stereotype.Controller",
        "org.springframework.stereotype.Service",
        "org.springframework.stereotype.Component",
        "org.springframework.stereotype.Repository",
    )
}

sourceSets {
    getByName("main") {
        java {
            srcDirs("src/main/java", "build/generated-jooq")
        }
        kotlin {
            srcDirs("src/main/kotlin")
        }
    }
}


dependencies {
    // Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // DB
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.jooq:jooq:3.19.10")
    implementation("org.flywaydb:flyway-core:10.16.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.16.0")

    jdbc("org.postgresql:postgresql")
    implementation("org.postgresql:postgresql")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

application {
    mainClass = "org.example.app.AppKt"
}
