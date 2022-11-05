plugins {
    `java-library`
}

val creekVersion : String by extra

dependencies {
    api(project(":metadata"))
    implementation("org.creekservice:creek-base-type:$creekVersion")
    implementation("org.creekservice:creek-observability-logging:$creekVersion")
}
