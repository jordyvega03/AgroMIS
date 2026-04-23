dependencies {
    implementation(project(":shared:shared-domain"))
    implementation(platform(rootProject.libs.quarkus.bom))
    implementation(rootProject.libs.quarkus.smallrye.reactive.messaging.kafka)
    implementation(rootProject.libs.quarkus.scheduler)
    implementation(rootProject.libs.quarkus.micrometer.registry.prometheus)
    implementation(rootProject.libs.avro)
    implementation(rootProject.libs.kafka.avro.serializer)
    testImplementation(rootProject.libs.testcontainers.redpanda)
}
