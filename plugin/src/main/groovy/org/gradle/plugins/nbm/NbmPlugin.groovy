package org.gradle.plugins.nbm

import org.gradle.api.GradleException;
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile

public class NbmPlugin implements Plugin<Project> {
    private static final String NBM_TASK = 'nbm'
    private static final String NETBEANS_TASK = 'netbeans'
    private static final String MANIFEST_TASK = 'generateModuleManifest'

    void apply(Project project) {
        project.tasks.add(NBM_TASK) << {
            throw new IllegalStateException(
                    "nbm is only valid when JavaPlugin is aplied; please update your build")
        }
        project.tasks.add(MANIFEST_TASK) << {
            throw new IllegalStateException(
                    "nbm is only valid when JavaPlugin is aplied; please update your build")
        }
        project.tasks.add(NETBEANS_TASK) << {
            throw new IllegalStateException(
                    "nbm is only valid when JavaPlugin is aplied; please update your build")
        }

        project.logger.info "Registering deferred NBM plugin configuration..."
        project.plugins.withType(JavaPlugin) { configure(project) }

        def convention = new NbmPluginConvention(project)
        project.convention.plugins.nbm = convention
        project.tasks.withType(NbmTask.class).all { NbmTask task ->
            task.conventionMapping.nbmBuildDir = { convention.nbmBuildDir }
        }
        project.tasks.withType(ModuleManifestTask.class).all { ModuleManifestTask task ->
            task.conventionMapping.generatedManifestFile = { convention.generatedManifestFile }
        }
        project.tasks.withType(NetBeansTask.class).all { NetBeansTask task ->
            task.conventionMapping.moduleBuildDir = { convention.moduleBuildDir }
        }
    }

    private configure(Project project) {
        project.logger.info "Configuring NBM plugin..."

        project.extensions.nbm = new NbmPluginExtension(project)

        ModuleManifestTask manifestTask = project.tasks.replace(MANIFEST_TASK, ModuleManifestTask)
        def userManifest = project.file('src' + File.separator + 'main' + File.separator + 'nbm' + File.separator + 'manifest.mf')
        if (userManifest.exists()) {
            project.tasks.jar.manifest.from { userManifest }
        }
        project.tasks.jar.manifest.from { manifestTask.getGeneratedManifestFile() }
        project.tasks.jar.dependsOn(manifestTask)
        
        // configure NBM task
        NetBeansTask netbeansTask = project.tasks.replace(NETBEANS_TASK, NetBeansTask)
        netbeansTask.dependsOn(project.tasks.jar)
        
        NbmTask nbmTask = project.tasks.replace(NBM_TASK, NbmTask)
        nbmTask.dependsOn(netbeansTask)

        setupPropertiesMerging(project)
    }

    private void setupPropertiesMerging(Project project) {
        def mergeTask = project.tasks.add('mergeProperties', MergePropertiesTask)
        project.tasks.findByName('jar').dependsOn(mergeTask)
        def generatedClasses = "${project.buildDir}/generated-resources/main"
        def generatedResources = "${project.buildDir}/generated-resources/resources"
        def generatedOutput = "${project.buildDir}/generated-resources/output"

        project.sourceSets.main.output.dir(generatedOutput, builtBy: 'mergeProperties')
        def compileJavaTask = project.tasks.getByName('compileJava')
        compileJavaTask.outputs.dir(generatedClasses)
        compileJavaTask.doLast { JavaCompile it ->
            new File(generatedClasses).mkdirs()
            project.copy {
                from project.sourceSets.main.output.classesDir
                into generatedClasses
                include '**/*.properties'
                includeEmptyDirs false
            }
            project.fileTree(dir: project.sourceSets.main.output.classesDir).include('**/*.properties').visit {
                if (!it.isDirectory()) {
                    it.file.delete()
                }
            }

        }
        Copy processResourcesTask = project.tasks.getByName('processResources')
        processResourcesTask.outputs.dir(generatedResources)
        processResourcesTask.doLast { Copy it ->
            new File(generatedResources).mkdirs()
            project.copy {
                from project.sourceSets.main.output.resourcesDir
                into generatedResources
                include '**/*.properties'
                includeEmptyDirs false
            }
            project.fileTree(dir: project.sourceSets.main.output.resourcesDir).include('**/*.properties').visit {
                if (!it.isDirectory()) {
                    it.file.delete()
                }
            }

        }
    }
}
