dependencies {
    implementation(project(":shared:shared-domain"))
    implementation(project(":contexts:prices:prices-domain"))
    implementation(project(":contexts:prices:prices-application"))
    implementation(project(":shared:shared-web"))
    implementation(project(":shared:shared-auth"))
    implementation(platform(rootProject.libs.quarkus.bom))
    implementation(rootProject.libs.quarkus.resteasy.reactive)
    implementation(rootProject.libs.quarkus.resteasy.reactive.jackson)
    testImplementation(project(":shared:shared-testing"))
}
