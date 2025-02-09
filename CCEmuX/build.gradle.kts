plugins {
	application
	id("com.gradleup.shadow") version "8.3.5"
	id("org.openjfx.javafxplugin") version "0.1.0"
}

val ccVersion: String by extra

allprojects {
	group = "net.clgd"
	version = "1.1.0" + if (System.getenv("GITHUB_SHA") == null) {
		""
	} else {
		"-${System.getenv("GITHUB_SHA")}"
	}

	repositories {
		mavenCentral()
		mavenLocal()
	}

	gradle.projectsEvaluated {
		java {
			toolchain {
				languageVersion.set(JavaLanguageVersion.of(17))
			}
		}

		tasks.withType(JavaCompile::class) {
			options.compilerArgs.addAll(listOf("-Xlint", "-Xlint:-processing"))
		}
	}
}

application {
	mainClass.set("net.clgd.ccemux.init.Launcher")
}

dependencies {
	implementation(project(":plugin-api"))

	runtimeOnly("org.slf4j:slf4j-simple:2.0.16")

	implementation("commons-cli:commons-cli:1.9.0")
	implementation("org.apache.commons:commons-lang3:3.17.0")
	implementation("org.fusesource.jansi:jansi:2.4.1")

	implementation("com.google.code.gson:gson:2.11.0")
	implementation("com.googlecode.lanterna:lanterna:3.1.3")

	compileOnly("com.google.auto.service:auto-service:1.1.1")
	annotationProcessor("com.google.auto.service:auto-service:1.1.1")

	testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

javafx {
	version = "19"
	modules = listOf("javafx.controls")
}

tasks.processResources {
	exclude("**/*.xcf") // GIMP images

	filesMatching("ccemux.version") {
		expand("version" to version, "cc_version" to ccVersion)
	}
}

tasks.jar {
	manifest {
		attributes(
			"SplashScreen-Image" to "img/splash2.gif",
			"Implementation-Version" to project.version,
		)
	}
}

tasks.shadowJar {
	archiveClassifier.set("cct")
	description = "A shadowed jar which bundles all dependencies"

	minimize {
		exclude(dependency("org.slf4j:.*:.*"))
	}

	mergeServiceFiles()
	append("META-INF/LICENSE")
	append("META-INF/LICENSE.txt")
	append("META-INF/NOTICE")
	append("META-INF/NOTICE.txt")

	exclude(listOf(
		// Exclude random junk
		"META-INF/maven/*/*/*.*",
	))
}

tasks.assemble.configure {
	dependsOn(tasks.shadowJar)
}

tasks.test {
	useJUnitPlatform()
}

tasks.withType(AbstractArchiveTask::class.java).configureEach {
	isPreserveFileTimestamps = false
	isReproducibleFileOrder = true
	filePermissions {}
	dirPermissions {}
}
