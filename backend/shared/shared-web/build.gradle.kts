dependencies {
    implementation(project(":shared:shared-domain"))
    implementation(project(":shared:shared-tenancy"))
    implementation(platform(rootProject.libs.quarkus.bom))
    implementation(rootProject.libs.quarkus.resteasy.reactive)
    implementation(rootProject.libs.quarkus.resteasy.reactive.jackson)
    implementation(rootProject.libs.quarkus.smallrye.openapi)
    implementation(rootProject.libs.quarkus.redis.client)
    implementation(rootProject.libs.quarkus.cache)
    implementation(rootProject.libs.bucket4j.core)
}
