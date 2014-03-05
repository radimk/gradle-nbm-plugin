package org.gradle.plugins.nbm

import org.apache.tools.ant.taskdefs.Taskdef
import org.apache.tools.ant.types.FileSet
import org.apache.tools.ant.types.Path
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class NetBeansTask extends ConventionTask {
    
    
    @OutputDirectory
    File moduleBuildDir

    private NbmPluginExtension netbeansExt() {
        project.extensions.nbm
    }
    
    @Input
    File getInputModuleJarFile() {
        project.tasks.jar.archivePath
    }

    @TaskAction
    void generate() {
        project.logger.info "NetBeansTask running"
        // nbmFile.write "Version: ${getVersion()}"
        def moduleDir = getModuleBuildDir()
        if (!moduleDir.isDirectory()) {
            moduleDir.mkdirs()
        }
        // TODO handle eager/autoload
        def modulesDir = new File(moduleDir, 'modules')

        def moduleJarName = netbeansExt().moduleName.replace('.', '-') + '.jar'
        project.copy { CopySpec it ->
            it.from(inputModuleJarFile)
            it.into(modulesDir)
            it.rename('.*\\.jar', moduleJarName)
        }

        def nbTask = antBuilder().antProject.createTask("genlist")
        nbTask.outputfiledir = moduleDir
        nbTask.module = "modules" + File.separator + moduleJarName
        FileSet fs = nbTask.createFileSet()
        fs.dir = moduleDir
        fs.setIncludes('**')
        nbTask.execute()
    }

    private AntBuilder antBuilder() {
        def antProject = ant.antProject
        ant.project.getBuildListeners().firstElement().setMessageOutputLevel(3)
        Taskdef taskdef = antProject.createTask("taskdef")
        taskdef.classname = "org.netbeans.nbbuild.MakeListOfNBM"
        taskdef.name = "genlist"
        taskdef.classpath = new Path(antProject, project.configurations.harness.asPath)
        taskdef.execute()
        return getAnt();
    }
}
