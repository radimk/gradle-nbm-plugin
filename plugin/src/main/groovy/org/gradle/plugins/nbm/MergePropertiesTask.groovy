package org.gradle.plugins.nbm

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class MergePropertiesTask extends ConventionTask {

    @InputFiles
    List<File> inputDirectories = []

    @OutputDirectory
    File outputDir

    private NbmPluginExtension netbeansExt() {
        project.extensions.nbm
    }

    @TaskAction
    void generate() {
        if (!outputDir.mkdirs() && !outputDir.isDirectory()) {
            throw new IOException("Failed to create generated resources output at ${outputDir}")
        }

        Set<String> paths = new HashSet<>()
        for (File input : inputDirectories) {
            def tree = project.fileTree(dir: input)
            tree.visit { if (!it.file.isDirectory()) paths.add(it.relativePath.pathString) }
        }

        paths.each { String path ->
            // if in both merge else copy
            def dest = new File(outputDir, path).parentFile
            dest.mkdirs()

            def inputFiles = []
            for (File input : inputDirectories) {
                def candidate = new File(input, path)
                if (candidate.exists())
                    inputFiles.add(candidate)
            }

            if (inputFiles.size() == 1) {
                project.copy {
                    from inputFiles.first()
                    into dest
                }
            } else {
                def destFile = new File(outputDir, path)
                def text = new StringBuilder()
                for (File file : inputFiles) {
                    if (text.size() > 0)
                        text.append('\n')

                    text.append(file.text)
                }
                destFile << text.toString()
            }
        }
    }
}
