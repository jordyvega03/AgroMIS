dependencies {
    implementation(project(":shared:shared-domain"))
    implementation(project(":contexts:projections:projections-domain"))
    implementation(project(":contexts:projections:projections-application"))
    implementation(project(":shared:shared-web"))
    implementation(project(":shared:shared-auth"))
    implementation(platform(rootProject.libs.quarkus.bom))
    implementation(rootProject.libs.quarkus.resteasy.reactive)
    implementation(rootProject.libs.quarkus.resteasy.reactive.jackson)
    testImplementation(project(":shared:shared-testing"))
}
