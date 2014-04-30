package org.gradle.plugins.nbm.integtest

import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.collect.Iterables
import org.gradle.tooling.BuildException
import org.gradle.tooling.model.GradleProject

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.zip.ZipFile

import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

class ManifestGenerationTest extends AbstractIntegrationTest {
    private static Path getProjectDir(GradleProject project) {
        return project.getBuildScript().getSourceFile().toPath().parent
    }

    private static Path getBuildDir(GradleProject project) {
        return getProjectDir(project).resolve('build')
    }

    private static Path getGeneratedManifestPath(GradleProject project) {
        return getBuildDir(project).resolve('generated-manifest.mf')
    }

    private static Map<String, String> getGeneratedModuleManifest(GradleProject project) {
        Path manifestPath = getGeneratedManifestPath(project)
        return ManifestUtils.readManifest(manifestPath)
    }

    def "check default generated manifest file"() {
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << """
apply plugin: org.gradle.plugins.nbm.NbmPlugin\n\
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
}
"""
        when:
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then:
        def manifest = checkDefaultModuleManifest(project)
        assert Strings.isNullOrEmpty(manifest.get('OpenIDE-Module-Implementation-Version'))
        assert !manifest.containsKey('OpenIDE-Module-Build-Version')
    }


    def "manifest file with implementation version"() {
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << """
apply plugin: org.gradle.plugins.nbm.NbmPlugin\n\
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
  implementationVersion = version
}
"""
        when:
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then:
        def manifest = checkDefaultModuleManifest(project)
        assert manifest.get('OpenIDE-Module-Implementation-Version') == '3.5.6'
        assert manifest.containsKey('OpenIDE-Module-Build-Version')
    }

    def checkDefaultModuleManifest(GradleProject project) {
        def manifest = getGeneratedModuleManifest(project)

        assert manifest.get('Manifest-Version') == '1.0'
        assert manifest.get('OpenIDE-Module-Specification-Version') == '3.5.6'
        assert manifest.get('OpenIDE-Module') == 'my-test-project'
        assert manifest.get('Created-By') == 'Gradle NBM plugin'
        manifest
    }
}