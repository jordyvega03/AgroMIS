plugins {
    alias(libs.plugins.versions.checker)
}

subprojects {
    apply(plugin = "agromis.conventions")
}
