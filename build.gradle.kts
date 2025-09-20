buildscript {
    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:10.20.1")
    }
}

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.5"
	id("io.spring.dependency-management") version "1.1.7"
    id("org.flywaydb.flyway") version "10.20.0"
    id("org.jooq.jooq-codegen-gradle") version "3.19.25"
    id("jacoco")
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Book management API"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

// gradle.propertiesからプロパティを読み込む
val dbUrl = project.property("db.url") as String
val dbUser = project.property("db.user") as String
val dbPassword = project.property("db.password") as String

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	runtimeOnly("org.postgresql:postgresql")
    jooqCodegen("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// 生成するJarファイル名を定義
tasks.named<Jar>("bootJar") {
    archiveFileName.set("app.jar")
}

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = dbUrl
            user = dbUser
            password = dbPassword
        }
        generator {
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                includes = ".*"
                excludes = "flyway_schema_history"
                inputSchema = "public"
            }
            target {
                packageName = "org.jooq.generated"
                directory = "build/generated-sources/jooq"
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs("build/generated-sources/jooq")
        }
    }
}

flyway {
    url = dbUrl
    user = dbUser
    password = dbPassword
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
    }
}