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
        buildFile << \
"""
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


    def "manifest file with layer file"() {

        given: "Build file with configured nbm plugin"
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin\n\
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'\n\
  layer = 'rootpckg/mypckg/subpckg/layer.xml'
}
"""
        when: "Generate netbeans module manifest"
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then: "Default manifest entries exist with correct values."
        def manifest = checkDefaultModuleManifest(project)
        
        then: "Entry 'OpenIDE-Module-Layer' exists in manifest with correct value."
        assert 'rootpckg/mypckg/subpckg/layer.xml' == manifest.get('OpenIDE-Module-Layer')
    }

    def "manifest file with autoupdateShowInClient"() {

        given: "Build file with configured nbm plugin"
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin\n\
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'\n\
  autoupdateShowInClient = false
}
"""
        when: "Generate netbeans module manifest"
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then: "Default manifest entries exist with correct values."
        def manifest = checkDefaultModuleManifest(project)

        then: "Entry 'AutoUpdate-Show-In-Client' exist in manifest with correct value."
        assert 'false' == manifest.get('AutoUpdate-Show-In-Client')
    }

    def "manifest file with implementation version"() {
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
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

    def "friend packages are added to manifest for sub packages"() {
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
  implementationVersion = version

  friendPackages {
    addWithSubPackages(sourceSets.main, 'rootpckg.mypckg')
  }
}
"""

        setupDefaultSources()

        when:
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then:
        def manifest = checkDefaultModuleManifest(project)
        assert manifest.get('OpenIDE-Module-Public-Packages') == 'rootpckg.mypckg.subpckg.*, rootpckg.mypckg.subpckg3.*'
    }

    def "friend packages are added to manifest for sub packages of root"() {
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
  implementationVersion = version

  friendPackages {
    addWithSubPackages(sourceSets.main, 'rootpckg')
  }
}
"""

        setupDefaultSources()

        when:
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then:
        def manifest = checkDefaultModuleManifest(project)
        assert manifest.get('OpenIDE-Module-Public-Packages') == 'rootpckg.mypckg.subpckg.*, rootpckg.mypckg.subpckg3.*'
    }

    def "friend packages are added explicitly"() {
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
  implementationVersion = version

  friendPackages {
    add 'rootpckg.mypckg'
    add 'rootpckg.mypckg.subpckg'
  }
}
"""

        setupDefaultSources()

        when:
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then:
        def manifest = checkDefaultModuleManifest(project)
        assert manifest.get('OpenIDE-Module-Public-Packages') == 'rootpckg.mypckg.*, rootpckg.mypckg.subpckg.*'
    }

    def "friend packages are added explicitly with starts"() {
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
  implementationVersion = version

  friendPackages {
    add 'rootpckg.mypckg.*'
    add 'rootpckg.mypckg.subpckg.*'
  }
}
"""

        setupDefaultSources()

        when:
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then:
        def manifest = checkDefaultModuleManifest(project)
        assert manifest.get('OpenIDE-Module-Public-Packages') == 'rootpckg.mypckg.*, rootpckg.mypckg.subpckg.*'
    }

    def setupDefaultSources() {
        createProjectFile('src', 'main', 'java', 'rootpckg', 'mypckg', 'subpckg', 'A.java') << \
"""
package rootpckg.mypckg.subpckg;
public class A { }
"""
        createProjectDir('src', 'main', 'java', 'rootpckg', 'mypckg', 'subpckg2')
        createProjectFile('src', 'main', 'java', 'rootpckg', 'mypckg', 'subpckg3', 'B.java') << \
"""
package rootpckg.mypckg.subpckg3;
public class B { }
"""
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