plugins {
	`java-library`
}

val ccVersion: String by extra

dependencies {
	api("cc.tweaked:cc-tweaked-1.20.1-core:$ccVersion")
	api("org.slf4j:slf4j-api:2.0.16")
	api("com.google.guava:guava:33.3.1-jre")
}
