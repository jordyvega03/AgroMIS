dependencies {
    implementation(project(":shared:shared-domain"))
    implementation(project(":contexts:prices:prices-domain"))
    implementation(project(":contexts:prices:prices-application"))
    implementation(project(":shared:shared-eventing"))
    implementation(project(":shared:shared-tenancy"))
    implementation(platform(rootProject.libs.quarkus.bom))
    implementation(rootProject.libs.quarkus.hibernate.orm.panache)
    implementation(rootProject.libs.quarkus.jdbc.postgresql)
    testImplementation(project(":shared:shared-testing"))
}
