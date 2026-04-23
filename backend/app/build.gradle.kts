plugins {
    id("io.quarkus")
}

dependencies {
    // Shared kernel
    implementation(project(":shared:shared-domain"))
    implementation(project(":shared:shared-eventing"))
    implementation(project(":shared:shared-tenancy"))
    implementation(project(":shared:shared-auth"))
    implementation(project(":shared:shared-web"))

    // Bounded contexts — interfaces layer
    implementation(project(":contexts:farmers:farmers-interfaces"))
    implementation(project(":contexts:reports:reports-interfaces"))
    implementation(project(":contexts:projections:projections-interfaces"))
    implementation(project(":contexts:prices:prices-interfaces"))
    implementation(project(":contexts:alerts:alerts-interfaces"))
    implementation(project(":contexts:buyers:buyers-interfaces"))
    implementation(project(":contexts:incentives:incentives-interfaces"))
    implementation(project(":contexts:gis:gis-interfaces"))
    implementation(project(":contexts:integration:integration-interfaces"))

    // Quarkus runtime
    implementation(enforcedPlatform(rootProject.libs.quarkus.bom))
    implementation(rootProject.libs.quarkus.resteasy.reactive)
    implementation(rootProject.libs.quarkus.resteasy.reactive.jackson)
    implementation(rootProject.libs.quarkus.hibernate.orm.panache)
    implementation(rootProject.libs.quarkus.jdbc.postgresql)
    implementation(rootProject.libs.quarkus.flyway)
    implementation(rootProject.libs.quarkus.smallrye.reactive.messaging.kafka)
    implementation(rootProject.libs.quarkus.oidc)
    implementation(rootProject.libs.quarkus.micrometer.registry.prometheus)
    implementation(rootProject.libs.quarkus.opentelemetry)
    implementation(rootProject.libs.quarkus.smallrye.health)
    implementation(rootProject.libs.quarkus.smallrye.openapi)
    implementation(rootProject.libs.quarkus.cache)
    implementation(rootProject.libs.quarkus.redis.client)
    implementation(rootProject.libs.quarkus.scheduler)

    testImplementation(project(":shared:shared-testing"))
}
