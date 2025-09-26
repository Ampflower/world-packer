plugins {
	java
	application
	alias(libs.plugins.shadow)
}

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath(libs.proguard)
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

application {
	mainClass = "gay.ampflower.worldpacker.Main"
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.bundles.commons)
	implementation(libs.bundles.utils)
	implementation(libs.bundles.logger)
	annotationProcessor(libs.bundles.annotations)
}

tasks {
	shadowJar {
		mergeServiceFiles()

		// We don't need package-info
		exclude("**/package-info.class")

		// We don't need SQL
		exclude("ch/qos/logback/classic/db/script/*")
	}

	// Using a well-known Java obfuscater as a tree shaker.
	val proguard = register<proguard.gradle.ProGuardTask>("proguard") {
		group = "build"
		dependsOn(shadowJar)

		inputs.property("awa", Math.random())

		injars(shadowJar.get().archiveFile.get())
		outjars("${layout.buildDirectory.get()}/libs/${project.name}-$version-proguard.jar")
		libraryjars("${System.getProperty("java.home")}/jmods/")

		// Jakarta is not used here.
		dontwarn("jakarta.**")

		// Groovy isn't here.
		dontwarn("org.codehaus.**")

		// pack200's dead, sorry.
		dontwarn("org.objectweb.**")
		dontwarn("org.apache.commons.compress.harmony.pack200.**")

		// We currently don't expose these.
		dontwarn("org.tukaani.**")
		dontwarn("com.github.luben.zstd.**")
		dontwarn("org.brotli.**")

		// Proguard bug - polymorphic bytecode is here
		dontwarn("java.lang.invoke.VarHandle")
		dontwarn("java.lang.invoke.MethodHandle")

		// We don't want this:tm:, that's not part of the goal.
		dontobfuscate()

		// This is technically obfuscation, and thus unwanted.
		dontoptimize()

		// If we were using servlets, this would be necessary, but it isn't.
		// keep("public class ch.qos.logback.classic.servlet.LogbackServletContainerInitializer {}")

		// Stuff ShadowJar likes killing and should not be killed.
		keep("public class ch.qos.logback.classic.spi.LogbackServiceProvider {}")

		// Proguard turns your logger into ghosts.
		// If for whatever reason anyone else in the future needs this, this may be helpful:
		//
		// -Dslf4j.internal.verbosity=debug
		// -Dslf4j.internal.report.stream=stderr
		// -Dlogback.debug=true
		// -Dlogback.statusListenerClass=ch.qos.logback.core.status.OnConsoleStatusListener

		keep("class ch.qos.logback.classic.util.DefaultJoranConfigurator {}")

		// These need their setters intact for Joran to work.
		keep("class ch.qos.logback.core.ConsoleAppender { public void set*(**); }")
		keep("class ch.qos.logback.core.OutputStreamAppender { public void set*(**); }")
		keep("class ch.qos.logback.classic.encoder.PatternLayoutEncoder { public void set*(**); }")

		// I've had problems in the past with this.
		keepclassmembers(mapOf("allowoptimization" to false), "enum * { public static **[] values(); public static ** valueOf(java.lang.String); }")

		// picocli requires this
		keepattributes("*Annotation*")

		keepclassmembers(mapOf("allowoptimization" to false), "class picocli.CommandLine\$AutoHelpMixin { private boolean *; }")

		// It helps if you keep this...
		keep(
			"""
			public class gay.ampflower.worldpacker.Main {
				@picocli.CommandLine${'$'}Parameter
				private * *;
				
				@picocli.CommandLine${'$'}Option
				private * *;
				
				public static void main(java.lang.String[]);
			}"""
		)
	}

	build {
		dependsOn(proguard)
	}
}