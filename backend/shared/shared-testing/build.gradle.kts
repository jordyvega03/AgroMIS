dependencies {
    api(platform("org.junit:junit-bom:5.10.2"))
    api("org.junit.jupiter:junit-jupiter")
    api(rootProject.libs.assertj.core)
    api(platform(rootProject.libs.quarkus.bom))
    api("io.quarkus:quarkus-test-common")
    api(rootProject.libs.testcontainers.postgresql)
    api(rootProject.libs.testcontainers.redpanda)
    api(rootProject.libs.testcontainers.keycloak)
    implementation(project(":shared:shared-domain"))
    implementation(project(":shared:shared-tenancy"))
}
