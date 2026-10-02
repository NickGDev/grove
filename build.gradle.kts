buildscript {
    dependencies {
        constraints {
            // spring-boot-buildpack-platform 4.1.1 pulls these on the plugin classpath;
            // project-level constraints below cannot reach it, but the SBOM includes it.
            classpath("tools.jackson.core:jackson-databind:3.2.3") {
                because("buildpack platform pulls 3.1.5 (Dependabot alerts 5-9)")
            }
            classpath("org.apache.commons:commons-lang3:3.21.0") {
                because("buildpack platform pulls 3.16.0 (Dependabot alert 1)")
            }
        }
    }
}

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "1.1.14"
    id("com.github.ben-manes.versions") version "0.64.0"
}

group = "ie.grove"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// Override vulnerable BOM-managed versions (Dependabot fixes)
extra["tomcat.version"] = "11.0.26"        // fixes CVE-2026-65905, CVE-2026-65182, CVE-2026-68525
extra["jackson-bom.version"] = "3.2.3"     // fixes CVE-2026-68497, CVE-2026-91777, CVE-2026-91776, CVE-2026-83557, CVE-2026-19032
extra["jackson-2-bom.version"] = "2.22.3"  // fasterxml line paired with jackson 3.2.x: databind 3.2.3 needs jackson-annotations 2.22 (JsonApplyView)
extra["commons-lang3.version"] = "3.21.0"  // CVE-2025-48924 fixed in 3.18.0; explicit pin

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-webauthn")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation("org.xerial:sqlite-jdbc")
    implementation("org.hibernate.orm:hibernate-community-dialects")
    runtimeOnly("org.postgresql:postgresql")
    // Flyway 10+ moved per-database support out of flyway-core; without this
    // module Flyway rejects any Postgres connection (SQLite ships built in).
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-mail-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    constraints {
        implementation("tools.jackson.core:jackson-databind:3.2.3") {
            because("fixes CVE-2026-68497, CVE-2026-91777, CVE-2026-91776, CVE-2026-83557, CVE-2026-19032")
        }
        implementation("org.apache.commons:commons-lang3:3.21.0") {
            because("fixes CVE-2025-48924")
        }
        implementation("org.apache.tomcat.embed:tomcat-embed-core:11.0.26") {
            because("fixes CVE-2026-65905, CVE-2026-65182, CVE-2026-68525")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    doFirst {
        layout.buildDirectory.get().asFile.mkdirs()
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("grove")
            mainClass.set("ie.grove.GroveApplication")
            buildArgs.addAll("--no-fallback", "-H:+ReportExceptionStackTraces")
        }
    }
}
