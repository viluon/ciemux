// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

plugins {
    id("cc-tweaked.java-convention")
    id("cc-tweaked.publishing")
    id("cc-tweaked.vanilla")
}

val mcVersion: String by extra

java {
    withJavadocJar()
}

dependencies {
    api(project(":core-api"))
}

tasks.javadoc {
    title = "CC: Tweaked $version Minecraft $mcVersion"
    include("dan200/computercraft/api/**/*.java")

    options {
        (this as StandardJavadocDocletOptions)

        groups = mapOf(
            "Common" to listOf(
                "dan200.computercraft.api",
                "dan200.computercraft.api.lua",
                "dan200.computercraft.api.peripheral",
            ),
            "Upgrades" to listOf(
                "dan200.computercraft.api.client.turtle",
                "dan200.computercraft.api.pocket",
                "dan200.computercraft.api.turtle",
                "dan200.computercraft.api.upgrades",
            ),
        )

        addBooleanOption("-allow-script-in-comments", true)
        bottom(
            """
            <script src="https://cdn.jsdelivr.net/npm/prismjs@v1.29.0/components/prism-core.min.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/prismjs@v1.29.0/plugins/autoloader/prism-autoloader.min.js"></script>
            <link href=" https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism.min.css " rel="stylesheet">
            """.trimIndent(),
        )
    }

    // Include the core-api in our javadoc export. This is wrong, but it means we can export a single javadoc dump.
    source(project(":core-api").sourceSets.main.map { it.allJava })
}
