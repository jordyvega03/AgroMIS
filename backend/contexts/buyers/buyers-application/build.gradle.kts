dependencies {
    implementation(project(":shared:shared-domain"))
    implementation(project(":contexts:buyers:buyers-domain"))
    implementation(project(":shared:shared-eventing"))
    implementation(platform(rootProject.libs.quarkus.bom))
    implementation(rootProject.libs.quarkus.arc)
    testImplementation(project(":shared:shared-testing"))
}
