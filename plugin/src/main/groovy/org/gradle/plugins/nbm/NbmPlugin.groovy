package org.gradle.plugins.nbm;

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.JavaPlugin

public class NbmPlugin implements Plugin<Project> {
  private static final String NBM_TASK = 'nbm'
  void apply(Project project) {
    project.tasks.add(NBM_TASK) << {
      throw new IllegalStateException(
                    "nbm is only valid when JavaPlugin is aplied; please update your build")
    }
    project.logger.info "Registering deferred NBM plugin configuration..."
    project.plugins.withType(JavaPlugin) { configure(project) }
  }
  
  private configure(Project project) {
    project.logger.info "Configuring Rebel plugin..."
    
    project.extensions.nbm = new NbmPluginExtension()
    
    // configure NBM task
    NbmTask nbmTask = project.tasks.replace(NBM_TASK, NbmTask)
    nbmTask.dependsOn(project.tasks.jar)
  }
}
