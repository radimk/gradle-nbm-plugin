package org.gradle.plugins.nbm

import org.apache.tools.ant.taskdefs.Taskdef
import org.apache.tools.ant.types.Path
import org.gradle.api.DefaultTask
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class NbmTask extends ConventionTask {

    @OutputFile
    File outputFile

    @OutputDirectory
    File nbmBuildDir

    private NbmPluginExtension netbeansExt() {
        project.extensions.nbm
    }

    @TaskAction
    void generate() {
        project.logger.info "NbmTask running"
        def nbmFile = getOutputFile()
//        if (!nbmFile.isFile()) {
//            nbmFile.parentFile.mkdirs()
//            nbmFile.createNewFile()
//        }
        def nbmDir = getNbmBuildDir()
        if (!nbmDir.isDirectory()) {
            nbmDir.mkdirs()
        }
        // nbmFile.write "Version: ${getVersion()}"

        def moduleJarName = netbeansExt().moduleName.replace('.', '-')

        def makenbm = antBuilder().antProject.createTask("makenbm")
        makenbm.productDir = new File(nbmDir, 'netbeans' + File.separator + 'extra') // TODO use cluster
        makenbm.file = nbmFile
        makenbm.module = "modules" + File.separator + moduleJarName + ".jar"
        makenbm.execute()
    }

    private AntBuilder antBuilder() {
        def antProject = ant.antProject
        ant.project.getBuildListeners().firstElement().setMessageOutputLevel(3)
        Taskdef taskdef = antProject.createTask("taskdef")
        taskdef.classname = "org.netbeans.nbbuild.MakeNBM"
        taskdef.name = "makenbm"
        taskdef.classpath = new Path(antProject, project.configurations.harness.asPath)
        taskdef.execute()
        return getAnt();
    }
}

