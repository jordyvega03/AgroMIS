dependencies {
    implementation(project(":shared:shared-domain"))
    implementation(project(":shared:shared-tenancy"))
    implementation(platform(rootProject.libs.quarkus.bom))
    implementation(rootProject.libs.quarkus.oidc)
    implementation(rootProject.libs.quarkus.arc)
    testImplementation(rootProject.libs.testcontainers.keycloak)
}
