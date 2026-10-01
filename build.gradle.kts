plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.11.1"
    id("com.github.ben-manes.versions") version "0.52.0"
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
extra["jackson-bom.version"] = "3.1.7"     // fixes CVE-2026-68497, CVE-2026-91777, CVE-2026-91776, CVE-2026-83557, CVE-2026-19032
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

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-mail-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
