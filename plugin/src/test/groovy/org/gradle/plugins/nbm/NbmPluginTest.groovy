package org.gradle.plugins.nbm

import com.google.common.collect.Sets
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.artifacts.configurations.Configurations
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

public class NbmPluginTest {

    // nbm plugin adds nbm task to project when JavaPlugin already applied
    @Test
    public void checkProjectTask() {
        Project project = ProjectBuilder.builder().build()
        project.project.plugins.apply(JavaPlugin)
        project.project.plugins.apply(NbmPlugin)

        def nbmTask = project.tasks.nbm
        assertNotNull(nbmTask)
        // assertTrue(task instanceof NbmTask)
        assertTrue(project.tasks.netbeans in nbmTask.dependsOn)
        def netbeansTask = project.tasks.netbeans
        assertTrue(project.tasks.jar in netbeansTask.dependsOn)
    }

    @Test
    public void createsConfigurations() {
        Project project = ProjectBuilder.builder().build()
        project.project.plugins.apply(JavaPlugin)
        project.project.plugins.apply(NbmPlugin)

        def configuration = project.configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
        assertThat(Configurations.getNames(configuration.extendsFrom),
            equalTo(Sets.newHashSet(NbmPlugin.PROVIDED_COMPILE_CONFIGURATION_NAME, NbmPlugin.IMPLEMENTATION_CONFIGURATION_NAME, NbmPlugin.BUNDLE_CONFIGURATION_NAME)))
        assertFalse(configuration.visible)
        assertTrue(configuration.transitive)

        configuration = project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        assertThat(Configurations.getNames(configuration.extendsFrom), equalTo(Sets.newHashSet(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, NbmPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME)))
        assertFalse(configuration.visible)
        assertTrue(configuration.transitive)

        configuration = project.configurations.getByName(NbmPlugin.PROVIDED_COMPILE_CONFIGURATION_NAME)
        assertThat(Configurations.getNames(configuration.extendsFrom), equalTo(Sets.newHashSet()))
        assertFalse(configuration.visible)
        assertTrue(configuration.transitive)

        configuration = project.configurations.getByName(NbmPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME)
        assertThat(Configurations.getNames(configuration.extendsFrom), equalTo(Sets.newHashSet(NbmPlugin.PROVIDED_COMPILE_CONFIGURATION_NAME)))
        assertFalse(configuration.visible)
        assertTrue(configuration.transitive)
    }

    // nbm plugin adds task to generate manifest used by JAR
    @Test
    public void checkManifest() {
        Project project = ProjectBuilder.builder().build()
        project.project.plugins.apply(JavaPlugin)
        project.project.plugins.apply(NbmPlugin)

        Task jarTask = project.tasks.find { jarTask -> jarTask.name == 'jar' }
        assertNotNull(jarTask)
        def manifestTasks = project.getTasks().withType(ModuleManifestTask)
        assertNotNull(manifestTasks)

        assertTrue(manifestTasks.iterator().next() in jarTask.dependsOn)
    }

    // nbm plugin hooks directories for merged properties
    @Test
    public void checkPluinDirectoryWithMergedProperties() {
        Project project = ProjectBuilder.builder().build()
        project.project.plugins.apply(JavaPlugin)
        project.project.plugins.apply(NbmPlugin)
        assertNotNull(project.project.sourceSets.main.output)
        assertTrue(project.tasks.getByName('compileJava').outputs.files.contains(project.file('build/generated-resources/main')))
        assertTrue(project.tasks.getByName('processResources').outputs.files.contains(project.file('build/generated-resources/resources')))
        assertTrue(project.tasks.getByName('mergeProperties').outputs.files.contains(project.file('build/generated-resources/output')))
    }

    // default module name is the project name.
    @Test
    public void checkModuleNameDefaults() {
        Project project = ProjectBuilder.builder().withName('my_test_project').build()
        project.project.plugins.apply(NbmPlugin)

        assertEquals(project.nbm.moduleName, 'my_test_project')
    }

    // default module name is the project name with dots instead of dashes.
    @Test
    public void checkModuleNameFormat() {
        Project project = ProjectBuilder.builder().withName('my-test-project').build()
        project.project.plugins.apply(NbmPlugin)

        assertEquals(project.nbm.moduleName, 'my.test.project')
    }

    // no implementation version by default
    @Test
    public void checkImplementationVersion() {
        Project project = ProjectBuilder.builder().withName('my-test-project').build()
        project.project.plugins.apply(NbmPlugin)

        assertNull(project.nbm.implementationVersion)
    }
}
