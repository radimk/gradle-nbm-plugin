package org.gradle.plugins.nbm.integtest

import org.gradle.tooling.model.GradleProject

import java.nio.file.Path

@SuppressWarnings('BlockStartsWithBlankLine')
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

        given: "Build file with configured nbm plugin (minimum configuration)"
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
}
"""
        when: "Generate netbeans module manifest"
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then: "Default manifest entries exist with correct values."
        def manifest = checkDefaultModuleManifest(project)

        then: "Entry 'OpenIDE-Module-Public-Packages' exist in manifest with correct value."
        assert '-' == manifest.get('OpenIDE-Module-Public-Packages')

        then: "Entry 'AutoUpdate-Show-In-Client' exist in manifest with correct value."
        assert manifest.get('AutoUpdate-Show-In-Client')

        then: "Entry 'OpenIDE-Module-Implementation-Version' exist in manifest with correct value."
        assert manifest.get('OpenIDE-Module-Implementation-Version') =~ /\d{12}/

        then: "Entry 'OpenIDE-Module-Build-Version' exist not in manifest"
        assert !manifest.containsKey('OpenIDE-Module-Build-Version')
    }

    def "manifest file with layer file"() {

        given: "Build file with configured nbm plugin"
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
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

    def "manifest file with java dependency"() {

        given: "Build file with configured nbm plugin"
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
  javaDependency = 'Java > 1.8'
}
"""
        when: "Generate netbeans module manifest"
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then: "Default manifest entries exist with correct values."
        def manifest = checkDefaultModuleManifest(project)

        then: "Entry 'OpenIDE-Module-Java-Dependencies' exists in manifest with correct value."
        assert manifest.get('OpenIDE-Module-Java-Dependencies') == 'Java > 1.8'
    }

    def "manifest file with autoupdateShowInClient"() {

        given: "Build file with configured nbm plugin"
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
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

    def "manifest file with configured implementation version and build version"() {

        given: "Build file with configured nbm plugin"
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
  implementationVersion = 'myImplVersion'
}
"""
        when: "Generate netbeans module manifest"
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then: "Default manifest entries exist with correct values."
        def manifest = checkDefaultModuleManifest(project)

        then: "Entry 'OpenIDE-Module-Implementation-Version' exist in manifest with correct value."
        assert manifest.get('OpenIDE-Module-Implementation-Version') == 'myImplVersion'

        then: "Entry 'OpenIDE-Module-Build-Version' exist in manifest with correct value."
        assert manifest.get('OpenIDE-Module-Build-Version') =~ /\d{12}/
    }

    def "manifest file with default implementation version and no build versions"() {

        given: "Build file with configured nbm plugin"
        // Set the moduleName because I have no idea what the project's name is,
        // so can't rely on the default value for that
        buildFile << \
"""
apply plugin: org.gradle.plugins.nbm.NbmPlugin
version = '3.5.6'
nbm {
  moduleName = 'my-test-project'
}
"""
        when: "Generate netbeans module manifest"
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then: "Default manifest entries exist with correct values."
        def manifest = checkDefaultModuleManifest(project)

        then: "Entry 'OpenIDE-Module-Implementation-Version' exists in manifest with correct value. (timestamp)"
        assert manifest.get('OpenIDE-Module-Implementation-Version') =~ /\d{12}/

        then: "Entry 'OpenIDE-Module-Build-Version' does not exist in manifest"
        assert !manifest.containsKey('OpenIDE-Module-Build-Version')
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

    def "friend packages are added explicitly with stars"() {
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

    def "friend packages are added explicitly with double stars"() {
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
    add 'rootpckg.mypckg.**'
    add 'rootpckg.mypckg.subpckg.**'
  }
}
"""

        setupDefaultSources()

        when:
        GradleProject project = runTasks(integTestDir, "generateModuleManifest")

        then:
        def manifest = checkDefaultModuleManifest(project)
        assert manifest.get('OpenIDE-Module-Public-Packages') == 'rootpckg.mypckg.**, rootpckg.mypckg.subpckg.**'
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

    private Map<String, String> checkDefaultModuleManifest(GradleProject project) {
        Map<String, String> manifest = getGeneratedModuleManifest(project)

        assert manifest.get('Manifest-Version') == '1.0'
        assert manifest.get('OpenIDE-Module-Specification-Version') == '3.5.6'
        assert manifest.get('OpenIDE-Module') == 'my-test-project'
        assert manifest.get('OpenIDE-Module-Requires')?.split(',')*.trim().contains('org.openide.modules.ModuleFormat1')
        assert manifest.get('Created-By') == 'Gradle NBM plugin'

        manifest
    }
}
