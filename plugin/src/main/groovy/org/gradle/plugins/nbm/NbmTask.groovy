package org.gradle.plugins.nbm

import org.gradle.api.DefaultTask
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class NbmTask extends ConventionTask {
//    @Input
//    String version

    @OutputFile
    File outputFile

    @TaskAction
    void generate() {
        project.logger.info "NbmTask running"
        def file = getOutputFile()
        if (!file.isFile()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        file.write "Version: ${getVersion()}"
    }
}

