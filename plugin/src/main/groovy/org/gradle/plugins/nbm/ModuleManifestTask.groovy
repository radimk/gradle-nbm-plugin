package org.gradle.plugins.nbm

import org.apache.tools.ant.taskdefs.Taskdef
import org.apache.tools.ant.types.Path
import org.gradle.api.DefaultTask
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.jar.Attributes
import java.util.jar.Manifest

class ModuleManifestTask extends ConventionTask {
    @Input
    String moduleName

    @OutputFile
    File generatedManifestFile

    @OutputDirectory
    File moduleBuildDir

    @TaskAction
    void generate() {
        def manifestFile = getGeneratedManifestFile()
        project.logger.info "Generating NetBeans module manifest $generatedManifestFile"
        def moduleDir = getModuleBuildDir()
        if (!moduleDir.isDirectory()) {
            moduleDir.mkdirs()
        }

        def manifest = new Manifest()
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, '1.0')
        manifest.getMainAttributes().put(new Attributes.Name('OpenIDE-Module'), moduleName)
        def os = new FileOutputStream(manifestFile)
        manifest.write(os)
        os.close()
    }
}

