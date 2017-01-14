package org.gradle.plugins.nbm

import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

class ModuleManifestTask extends ConventionTask {
    @OutputFile
    File generatedManifestFile

    public ModuleManifestTask() {
        outputs.upToDateWhen { checkUpToDate() }
    }

    private NbmPluginExtension netbeansExt() {
        project.extensions.nbm
    }

    public boolean checkUpToDate() {
        byte[] actualBytes = tryGetCurrentGeneratedContent()
        if (actualBytes == null) {
            return false
        }

        def output = new ByteArrayOutputStream(4096)
        getManifest().write(output)

        byte[] expectedBytes = output.toByteArray()
        return Arrays.equals(actualBytes, expectedBytes)
    }

    private byte[] tryGetCurrentGeneratedContent() {
        def manifestFile = getGeneratedManifestFile().toPath()
        if (!Files.isRegularFile(manifestFile)) {
            return null
        }

        try {
            return Files.readAllBytes(manifestFile)
        } catch (IOException ex) {
            return null;
        }
    }

    private String getBuildDate() {
        Date now = new Date(System.currentTimeMillis())
        def format = new SimpleDateFormat("yyyyMMddHHmm")
        return format.format(now)
    }

    private Map<String, String> getManifestEntries() {
        Map<String, String> result = new HashMap<String, String>()

        Map<String, String> moduleDeps = new HashMap<>()

        def mainSourceSet = project.sourceSets.main
        def compileConfig = project.configurations.findByName(mainSourceSet.compileConfigurationName).resolvedConfiguration

        HashSet<ResolvedArtifact> implArtifacts = new HashSet<>()
        project.configurations.nbimplementation.resolvedConfiguration.firstLevelModuleDependencies.each { ResolvedDependency it ->
            implArtifacts.addAll(it.moduleArtifacts)
        }

        HashSet<ResolvedArtifact> bundleArtifacts = new HashSet<>()
        project.configurations.bundle.resolvedConfiguration.firstLevelModuleDependencies.each { ResolvedDependency it ->
            bundleArtifacts.addAll(it.moduleArtifacts)
        }

        compileConfig.firstLevelModuleDependencies.each { ResolvedDependency it ->
            // println 'module ' + it.name + ', ' + it.id.id
            it.moduleArtifacts.each { a ->
                // println '  artifact ' + a + ' file ' + a.file
                if (a.file?.exists() && 'jar' == a.extension) {
                    JarFile jar = new JarFile(a.file)
                    def attrs = jar.manifest?.mainAttributes
                    def bundleName = attrs?.getValue(new Attributes.Name('Bundle-SymbolicName'))
                    if(bundleName && bundleArtifacts.contains(a)) {
                        moduleDeps.put(bundleName.split(';').first(), '')
                    } else {
                        def moduleName = attrs?.getValue(new Attributes.Name('OpenIDE-Module'))
                        def moduleVersion = attrs?.getValue(new Attributes.Name('OpenIDE-Module-Specification-Version'))
                        def implVersion = attrs?.getValue(new Attributes.Name('OpenIDE-Module-Implementation-Version'))
                        if(moduleName && moduleVersion) {
                            if(implArtifacts.contains(a))
                                moduleDeps.put(moduleName, " = $implVersion")
                            else
                                moduleDeps.put(moduleName, " > $moduleVersion")
                        }
                    }
                }
            }
        }

        result.put('Manifest-Version', '1.0')

        def classpath = computeClasspath()
        if (classpath != null && !classpath.isEmpty()) {
            result.put('Class-Path', classpath)
        }

        if (!moduleDeps.isEmpty())
            result.put(
                    'OpenIDE-Module-Module-Dependencies',
                    moduleDeps.entrySet().collect { it.key + it.value }.join(', ')
            )

        result.put('Created-By', 'Gradle NBM plugin')

        def requires = netbeansExt().requires;
        if (!requires.isEmpty()) {
            result.put('OpenIDE-Module-Requires', requires.join(', '))
        }

        def localizingBundle = netbeansExt().localizingBundle
        if (localizingBundle) {
            result.put('OpenIDE-Module-Localizing-Bundle', localizingBundle)
        }

        result.put('OpenIDE-Module', netbeansExt().moduleName)

        def implVersion = netbeansExt().implementationVersion
        if (implVersion) {
            result.put('OpenIDE-Module-Implementation-Version', implVersion)
            result.put('OpenIDE-Module-Build-Version', getBuildDate())
        } else {
            result.put('OpenIDE-Module-Implementation-Version', '')
        }
        result.put('OpenIDE-Module-Specification-Version', netbeansExt().specificationVersion)

        def packageList = netbeansExt().friendPackages.packageListPattern
        if (!packageList.isEmpty()) {
            Set packageListSet = new HashSet(packageList)
            def packages = packageListSet.toArray()
            Arrays.sort(packages) // because why not
            result.put('OpenIDE-Module-Public-Packages', packages.join(', '))
        }

        def moduleInstall = netbeansExt().moduleInstall
        if (moduleInstall) {
            result.put('OpenIDE-Module-Install', moduleInstall.replace('.', '/') + '.class')
        }

        return result
    }

    private Manifest getManifest() {
        // TODO: It would be nice to output manifest entries in the order they
        //   were specified.

        def manifest = new Manifest()
        def mainAttributes = manifest.mainAttributes

        getManifestEntries().each { key, value ->
            println 'add manifest entry ' + key + ': ' + value + ' / ' + (value == null)
            mainAttributes.put(new Attributes.Name(key), value)
        }
        return manifest
    }

    @TaskAction
    void generate() {
        def manifestFile = getGeneratedManifestFile()
        project.logger.info "Generating NetBeans module manifest $manifestFile"

        def os = new FileOutputStream(manifestFile)
        try {
            getManifest().write(os)
        } finally {
            os.close()
        }
    }

    private String computeClasspath() {
        FileCollection classpath = project.tasks.findByPath('netbeans').classpath
        def jarNames = [] as Set
        classpath.asFileTree.visit { FileVisitDetails fvd ->
            if (fvd.directory) return
            if (!fvd.name.endsWith('jar')) return

            JarFile jar = new JarFile(fvd.file)
            def attrs = jar.manifest?.mainAttributes
            def attrValue = attrs?.getValue(new Attributes.Name('OpenIDE-Module'))
            if (attrValue != null) return

            // JAR but not NetBeans module
            jarNames += 'ext/' + fvd.name
        }
        jarNames.join(' ')
    }
}
