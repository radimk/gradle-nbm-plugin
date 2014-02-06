package org.gradle.plugins.nbm

import org.gradle.api.Project

class NbmPluginConvention {
    final Project project

    NbmPluginConvention(Project project) {
        this.project = project
    }

    File getOutputFile() {
        project.file("$project.buildDir/gradle-module.nbm")
    }
}
