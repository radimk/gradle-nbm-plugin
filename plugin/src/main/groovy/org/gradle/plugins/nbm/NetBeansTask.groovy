package org.gradle.plugins.nbm
import org.apache.tools.ant.taskdefs.Taskdef
import org.apache.tools.ant.types.FileSet
import org.apache.tools.ant.types.Path
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTreeElement
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.*

import java.util.jar.Attributes
import java.util.jar.JarFile

class NetBeansTask extends ConventionTask {
    public static final String TEST_USER_DIR_NAME = 'testuserdir'

    private FileCollection classpath

    @OutputDirectory
    File moduleBuildDir

    private NbmPluginExtension netbeansExt() {
        project.extensions.nbm
    }

    @InputFile
    File getInputModuleJarFile() {
        project.tasks.jar.archivePath
    }

    @InputFiles @Optional
    FileCollection getClasspath() {
        return classpath
    }

    /**
     * Sets the classpath to include in the module content.
     *
     * @param classpath The classpath. Must not be null.
     */
    void setClasspath(Object classpath) {
        this.classpath = project.files(classpath)
    }

    /**
     * Adds files to the classpath to include in the module content.
     *
     * @param classpath The files to add. These are evaluated as per {@link org.gradle.api.Project#files(Object [])}
     */
    void classpath(Object... classpath) {
        FileCollection oldClasspath = getClasspath()
        this.classpath = project.files(oldClasspath ?: [], classpath)
    }

    private File getCacheDir() {
        File result = project.buildDir
        result = new File(result, TEST_USER_DIR_NAME)
        result = new File(result, 'var')
        result = new File(result, 'cache')
        return result
    }

    @TaskAction
    void generate() {
        project.logger.info "NetBeansTask running"
        // nbmFile.write "Version: ${getVersion()}"
        def moduleDir = getModuleBuildDir()
        if (!moduleDir.isDirectory()) {
            moduleDir.mkdirs()
        }
        def timestamp = new File(moduleDir, ".lastModified")
        if (timestamp.exists()) {
            timestamp.setLastModified(System.currentTimeMillis())
        } else {
            timestamp.createNewFile()
        }
        
        def modulesDir = new File(moduleDir, 'modules')
        
        def classpathExtFolder = netbeansExt().classpathExtFolder
        def modulesExtDir = new File(modulesDir, 'ext' + (classpathExtFolder ? "/$classpathExtFolder" : ""))

        project.delete(getCacheDir())

        def moduleJarName = netbeansExt().moduleName.replace('.', '-') + '.jar'
        project.copy { CopySpec it ->
            it.from(inputModuleJarFile)
            it.into(modulesDir)
            it.rename('.*\\.jar', moduleJarName)
        }

        project.copy { CopySpec it ->
            it.from(classpath)
            it.into(modulesExtDir)
            it.exclude { FileTreeElement fte ->
                if (fte.directory) return true
                if (!fte.name.endsWith('jar')) return true

                JarFile jar = new JarFile(fte.file)
                def attrs = jar.manifest?.mainAttributes
                def attrValue = attrs?.getValue(new Attributes.Name('OpenIDE-Module'))
                attrValue != null
            }
        }

        AntBuilder antBuilder = antBuilder()
        def moduleXmlTask = antBuilder.antProject.createTask('module-xml')
        moduleXmlTask.xmldir = new File(moduleDir, 'config' + File.separator + 'Modules')
        FileSet moduleFileSet = new FileSet()
        moduleFileSet.setDir(moduleDir)
        moduleFileSet.setIncludes('modules' + File.separator + moduleJarName)
        
        if(netbeansExt().isAutoload()) {
            moduleXmlTask.addAutoload(moduleFileSet)
        }else if(netbeansExt().isEager()) {
            moduleXmlTask.addEager(moduleFileSet)
        }else {
            moduleXmlTask.addEnabled(moduleFileSet)
        }
        
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
        taskdef.classpath = new Path(antProject, netbeansExt().harnessConfiguration.asPath)
        taskdef.execute()
        Taskdef taskdef2 = antProject.createTask("taskdef")
        taskdef2.classname = "org.netbeans.nbbuild.CreateModuleXML"
        taskdef2.name = "module-xml"
        taskdef2.classpath = new Path(antProject, netbeansExt().harnessConfiguration.asPath)
        taskdef2.execute()
        return getAnt();
    }
}

