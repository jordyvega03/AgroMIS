rootProject.name = "agromis-backend"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Confluent Schema Registry artifacts
        maven { url = uri("https://packages.confluent.io/maven/") }
        // Testcontainers Keycloak
        maven { url = uri("https://jitpack.io") }
    }
}

// ---- Shared kernel -------------------------------------------------------
include(
    ":shared:shared-domain",
    ":shared:shared-eventing",
    ":shared:shared-tenancy",
    ":shared:shared-auth",
    ":shared:shared-testing",
    ":shared:shared-web",
)

// ---- Bounded contexts ----------------------------------------------------
val contexts = listOf(
    "farmers",
    "reports",
    "projections",
    "prices",
    "alerts",
    "buyers",
    "incentives",
    "gis",
    "integration",
)

val subModules = listOf("domain", "application", "infrastructure", "interfaces")

contexts.forEach { ctx ->
    subModules.forEach { sub ->
        include(":contexts:$ctx:$ctx-$sub")
    }
}

// ---- App + migrations ----------------------------------------------------
include(
    ":app",
    ":migrations",
)
