package org.gradle.plugins.nbm;

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.*

public class NbmPluginTest {

    @Test
    public void 'fails without harness property'() {
        Project project = ProjectBuilder.builder().build()
        try {
            project.project.plugins.apply(NbmPlugin)
            project.project.plugins.apply(JavaPlugin)
            fail('should fail if harness location is not known')
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void 'nbm plugin adds nbm task to project when JavaPlugin already applied'() {
        println('harness dir ' + System.getProperty('test.netbeans.harness.dir'))
        assertNotNull(System.getProperty('test.netbeans.harness.dir'))
        Project project = ProjectBuilder.builder().build()
        project.ext.netBeansHarnessDir = System.getProperty('test.netbeans.harness.dir')
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
        println('harness dir ' + System.getProperty('test.netbeans.harness.dir'))
        assertNotNull(System.getProperty('test.netbeans.harness.dir'))
        Project project = ProjectBuilder.builder().build()
        project.ext.netBeansHarnessDir = System.getProperty('test.netbeans.harness.dir')
        project.project.plugins.apply(JavaPlugin)
        project.project.plugins.apply(NbmPlugin)

        Task jarTask = project.tasks.find { jarTask -> jarTask.name == 'jar' }
        assertNotNull(jarTask)
        def manifestTasks = project.getTasks().withType(ModuleManifestTask)
        assertNotNull(manifestTasks)
        
        assertTrue(manifestTasks.iterator().next() in jarTask.dependsOn)
    }
}