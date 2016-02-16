package org.gradle.plugins.nbm

import com.google.common.collect.Sets
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.artifacts.configurations.Configurations
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.hamcrest.Matchers.*
import static org.junit.Assert.*

public class NbmPluginTest {

    @Test
    public void 'nbm plugin adds nbm task to project when JavaPlugin already applied'() {
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

    @Test public void createsConfigurations() {
        Project project = ProjectBuilder.builder().build()
        project.project.plugins.apply(JavaPlugin)
        project.project.plugins.apply(NbmPlugin)

        def configuration = project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME)
        assertThat(Configurations.getNames(configuration.extendsFrom),
                equalTo(Sets.newHashSet(NbmPlugin.PROVIDED_COMPILE_CONFIGURATION_NAME, NbmPlugin.IMPLEMENTATION_CONFIGURATION_NAME, NbmPlugin.BUNDLE_CONFIGURATION_NAME)))
        assertFalse(configuration.visible)
        assertTrue(configuration.transitive)

        configuration = project.configurations.getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME)
        assertThat(Configurations.getNames(configuration.extendsFrom), equalTo(Sets.newHashSet(JavaPlugin.COMPILE_CONFIGURATION_NAME, NbmPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME)))
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

    @Test
    public void 'nbm plugin adds task to generate manifest used by JAR'() {
        Project project = ProjectBuilder.builder().build()
        project.project.plugins.apply(JavaPlugin)
        project.project.plugins.apply(NbmPlugin)

        Task jarTask = project.tasks.find { jarTask -> jarTask.name == 'jar' }
        assertNotNull(jarTask)
        def manifestTasks = project.getTasks().withType(ModuleManifestTask)
        assertNotNull(manifestTasks)

        assertTrue(manifestTasks.iterator().next() in jarTask.dependsOn)
    }

    @Test
    public void 'nbm plugin hooks directories for merged properties'() {
        Project project = ProjectBuilder.builder().build()
        project.project.plugins.apply(JavaPlugin)
        project.project.plugins.apply(NbmPlugin)
        assertNotNull(project.project.sourceSets.main.output)
        assertTrue(project.tasks.getByName('compileJava').outputs.files.contains(project.file('build/generated-resources/main')))
        assertTrue(project.tasks.getByName('processResources').outputs.files.contains(project.file('build/generated-resources/resources')))
        assertTrue(project.tasks.getByName('mergeProperties').outputs.files.contains(project.file('build/generated-resources/output')))
    }

    @Test
    public void 'default module name is the project name.'() {
        Project project = ProjectBuilder.builder().withName('my-test-project').build()
        project.project.plugins.apply(NbmPlugin)

        assertEquals(project.nbm.moduleName, 'my-test-project')
    }

    @Test
    public void 'no implementation version by default'() {
        Project project = ProjectBuilder.builder().withName('my-test-project').build()
        project.project.plugins.apply(NbmPlugin)

        assertNull(project.nbm.implementationVersion)
    }
}