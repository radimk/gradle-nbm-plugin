package org.gradle.plugins.nbm

import org.apache.tools.ant.taskdefs.Taskdef
import org.apache.tools.ant.types.Path
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
        // file.write "Version: ${getVersion()}"

        def makenbm = antBuilder().antProject.createTask("makenbm")
        makenbm.execute()
    }

    private AntBuilder antBuilder() {
        def antProject = ant.antProject
        Taskdef taskdef = antProject.createTask("taskdef")
        taskdef.classname = "org.netbeans.nbbuild.MakeNBM"
        taskdef.name = "makenbm"
        taskdef.classpath = new Path(antProject, project.configurations.harness.asPath)
        taskdef.execute()
        return getAnt();
    }
}

