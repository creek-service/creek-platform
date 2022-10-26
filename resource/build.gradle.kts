plugins {
    `java-library`
}

val creekBaseVersion : String by extra
val creekObsVersion : String by extra

dependencies {
    api(project(":metadata"))
    implementation("org.creekservice:creek-base-type:$creekBaseVersion")
    implementation("org.creekservice:creek-observability-logging:$creekObsVersion")
}
