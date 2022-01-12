/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.plugins.nbm.integtest

import org.apache.xml.resolver.CatalogManager
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.GradleProject
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.fail

/**
 * Abstract integration test using Gradle's tooling API.
 *
 * @author Benjamin Muschko
 */
abstract class AbstractIntegrationTest extends Specification {
    File integTestDir
    File buildFile
    File gradlePropsFile
    CatalogManager cm

    String nbVersion = "RELEASE124"

    def setup() {
        def buildDir = Path.of("build").toAbsolutePath()
        def testRoot = buildDir.resolve("integTest")
        Files.createDirectories(testRoot)
        integTestDir = Files.createTempDirectory(testRoot, "test-").toFile()

        buildFile = createNewFile(integTestDir, 'build.gradle')

        buildFile << \
"""
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath files('${buildDir.resolve("classes/groovy/main")}')
        classpath files('${buildDir.resolve("resources/main")}')
    }
}

repositories {
    mavenCentral()
}
"""
        File settingsFile = createNewFile(integTestDir, 'settings.gradle')
        settingsFile << ''

        gradlePropsFile = createNewFile(integTestDir, 'gradle.properties')

        cm = new CatalogManager()
        cm.setVerbosity(9)
    }

    def cleanup() {
        integTestDir.deleteDir()
    }

    protected File createNewDir(File parent, String dirname) {
        File dir = new File(parent, dirname)
        ensureDirectory(dir)
        dir
    }

    protected File createNewFile(File parent, String filename) {
        File file = new File(parent, filename)

        if (!file.exists()) {
            if (!file.createNewFile()) {
                fail("Unable to create new test file $file.canonicalPath.")
            }
        }

        file
    }

    private static void ensureDirectory(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                fail("Unable to create new test directory $dir.canonicalPath.")
            }
        }
    }

    private static File subPath(File root, String... childParts) {
        File file = root
        for (String part : childParts) {
            file = new File(file, part)
        }
        file
    }

    protected File createProjectDir(String... pathParts) {
        File dir = subPath(integTestDir, pathParts)
        ensureDirectory(dir)
        dir
    }

    protected File createProjectFile(String... pathParts) {
        File file = subPath(integTestDir, pathParts)
        ensureDirectory(file.parentFile)

        if (!file.exists()) {
            if (!file.createNewFile()) {
                fail("Unable to create new test file $file.canonicalPath.")
            }
        }

        file
    }

    protected void assertExistingFiles(File dir, List<String> requiredFilenames) {
        assertExistingDirectory(dir)
        def dirFileNames = dir.listFiles()*.name

        requiredFilenames.each { filename ->
            assert dirFileNames.find { it ==~ filename }
        }
    }

    protected void assertNoSignatureFiles(File dir) {
        assertExistingDirectory(dir)
        def dirFileNames = dir.listFiles()*.name

        dirFileNames.each { filename ->
            assert !filename.endsWith('.asc')
        }
    }

    private void assertExistingDirectory(File dir) {
        if (!dir || !dir.exists()) {
            fail("Unable to check target directory '${dir?.canonicalPath}' for files.")
        }
    }

    protected GradleProject runTasks(File projectDir, String... tasks) {
        ProjectConnection connection = GradleConnector.newConnector().forProjectDirectory(projectDir).connect()

        try {
            BuildLauncher builder = connection.newBuild()
            builder.forTasks(tasks).run()
            return connection.getModel(GradleProject)
        }
        finally {
            connection?.close()
        }
    }
}
