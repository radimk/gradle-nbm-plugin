package org.gradle.plugins.nbm

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

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
}