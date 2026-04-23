dependencies {
    implementation(project(":shared:shared-domain"))
    implementation(platform(rootProject.libs.quarkus.bom))
    implementation(rootProject.libs.quarkus.resteasy.reactive)
    implementation(rootProject.libs.quarkus.oidc)
    implementation(rootProject.libs.quarkus.arc)
    implementation(rootProject.libs.quarkus.smallrye.reactive.messaging.kafka)
}
