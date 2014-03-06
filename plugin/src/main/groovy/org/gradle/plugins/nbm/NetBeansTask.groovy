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

        AntBuilder antBuilder = antBuilder()
        def moduleXmlTask = antBuilder.antProject.createTask('module-xml')
        moduleXmlTask.xmldir = new File(moduleDir, 'config' + File.separator + 'Modules')
        FileSet moduleFileSet = new FileSet()
        moduleFileSet.setDir(moduleDir)
        moduleFileSet.setIncludes('modules' + File.separator + moduleJarName)
        moduleXmlTask.addEnabled(moduleFileSet)
        moduleXmlTask.execute()

        def nbTask = antBuilder.antProject.createTask('genlist')
        nbTask.outputfiledir = moduleDir
        nbTask.module = 'modules' + File.separator + moduleJarName
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
        Taskdef taskdef2 = antProject.createTask("taskdef")
        taskdef2.classname = "org.netbeans.nbbuild.CreateModuleXML"
        taskdef2.name = "module-xml"
        taskdef2.classpath = new Path(antProject, project.configurations.harness.asPath)
        taskdef2.execute()
        return getAnt();
    }
}

