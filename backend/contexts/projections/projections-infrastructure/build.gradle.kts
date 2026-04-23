dependencies {
    implementation(project(":shared:shared-domain"))
    implementation(project(":contexts:projections:projections-domain"))
    implementation(project(":contexts:projections:projections-application"))
    implementation(project(":shared:shared-eventing"))
    implementation(project(":shared:shared-tenancy"))
    implementation(platform(rootProject.libs.quarkus.bom))
    implementation(rootProject.libs.quarkus.hibernate.orm.panache)
    implementation(rootProject.libs.quarkus.jdbc.postgresql)
    testImplementation(project(":shared:shared-testing"))
}
