plugins {
    `java-library`
}

val creekBaseVersion : String by extra

dependencies {
    api(project(":metadata"))
    implementation("org.creekservice:creek-base-type:$creekBaseVersion")
}
