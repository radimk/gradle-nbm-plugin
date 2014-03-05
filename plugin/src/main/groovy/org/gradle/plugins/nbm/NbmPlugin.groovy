package org.gradle.plugins.nbm

import org.gradle.api.GradleException;
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.JavaPlugin

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
//            task.conventionMapping.version = { convention.outputVersion ?: project.version }
            task.conventionMapping.outputFile = { convention.outputFile }
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

        project.extensions.nbm = new NbmPluginExtension()

        if (!project.hasProperty("netBeansHarnessDir")) {
            throw new GradleException('netBeansHarnessDir property is not set.')
        }
        project.extensions.nbm.harnessDir = new File(project.property("netBeansHarnessDir"))

        ModuleManifestTask manifestTask = project.tasks.replace(MANIFEST_TASK, ModuleManifestTask)
        project.tasks.jar.manifest.from { manifestTask.generatedManifestFile }
        project.tasks.jar.dependsOn(manifestTask)
        
        // configure NBM task
        NetBeansTask netbeansTask = project.tasks.replace(NETBEANS_TASK, NetBeansTask)
        netbeansTask.dependsOn(project.tasks.jar)
        
        NbmTask nbmTask = project.tasks.replace(NBM_TASK, NbmTask)
        nbmTask.dependsOn(netbeansTask)
    }
}
