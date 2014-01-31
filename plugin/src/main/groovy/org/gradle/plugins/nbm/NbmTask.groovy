package org.gradle.plugins.nbm

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class NbmTask extends DefaultTask {
  
  @TaskAction
  def generate() {
    project.logger.info "NbmTask running"

  }
}

