// Modulo sin dependencias de framework — DDD building blocks puros
plugins {
    java
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(rootProject.libs.assertj.core)
}
