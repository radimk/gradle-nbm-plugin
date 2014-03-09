package org.gradle.plugins.nbm

import org.apache.tools.ant.taskdefs.Taskdef
import org.apache.tools.ant.types.Path
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

class ModuleManifestTask extends ConventionTask {
    @OutputFile
    File generatedManifestFile

    private NbmPluginExtension netbeansExt() {
        project.extensions.nbm
    }

    @TaskAction
    void generate() {
        def manifestFile = getGeneratedManifestFile()
        project.logger.info "Generating NetBeans module manifest $generatedManifestFile"

        Map<String, String> moduleDeps = new HashMap<>()
        def mainSourceSet = project.sourceSets.main
        def compileConfig = project.configurations.findByName(mainSourceSet.compileConfigurationName)
        def resolvedConfiguration = compileConfig.resolvedConfiguration
        resolvedConfiguration.firstLevelModuleDependencies.each { ResolvedDependency it ->
            // println 'module ' + it.name + ', ' + it.id.id
            it.moduleArtifacts.each { a ->
                // println '  artifact ' + a + ' file ' + a.file
                if (a.file?.exists() && 'jar' == a.extension) {
                    JarFile jar = new JarFile(a.file)
                    def attrs = jar.manifest?.mainAttributes
                    def moduleName = attrs?.getValue(new Attributes.Name('OpenIDE-Module'))
                    def moduleVersion = attrs?.getValue(new Attributes.Name('OpenIDE-Module-Specification-Version'))
                    if (moduleName && moduleVersion) {
                        moduleDeps.put(moduleName, moduleVersion)
                    }
                }
            }
        }

        def manifest = new Manifest()
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, '1.0')
        manifest.getMainAttributes().put(new Attributes.Name('OpenIDE-Module'), netbeansExt().moduleName)
        manifest.getMainAttributes().put(new Attributes.Name('OpenIDE-Module-Requires'), 'org.openide.modules.ModuleFormat1')
        if (netbeansExt().specificationVersion) {
            manifest.getMainAttributes().put(new Attributes.Name('OpenIDE-Module-Specification-Version'), netbeansExt().specificationVersion)
        }
        if (!moduleDeps.isEmpty()) {
            manifest.getMainAttributes().put(
                    new Attributes.Name('OpenIDE-Module-Module-Dependencies'),
                    moduleDeps.entrySet().collect { it.key + ' > ' + it.value }.join(', '))
        }
        def os = new FileOutputStream(manifestFile)
        manifest.write(os)
        os.close()
    }
}
