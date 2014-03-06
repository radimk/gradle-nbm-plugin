package org.gradle.plugins.nbm.integtest

import org.gradle.tooling.BuildException
import org.gradle.tooling.model.GradleProject
import static org.hamcrest.MatcherAssert.*

class StandaloneNbmProjectTest extends AbstractIntegrationTest {
    def "load project"() {
        buildFile << """
apply plugin: 'java'
apply plugin: org.gradle.plugins.nbm.NbmPlugin

"""
        when:
        GradleProject project = runTasks(integTestDir, "tasks")

        then:
        project != null
        project.tasks.find { it.name == 'nbm'} != null
    }

    def "run nbm without module name "() {
        buildFile << """
apply plugin: 'java'
apply plugin: org.gradle.plugins.nbm.NbmPlugin

"""
        when:
        GradleProject project = runTasks(integTestDir, "nbm")

        then:
        def e = thrown(BuildException)
        // deepcause is groovy.lang.MissingPropertyException: Could not find property 'MODULE_NAME' on root project 'integTest'
        e != null
    }

    def "run nbm"() {
        buildFile << """
apply plugin: 'java'
apply plugin: org.gradle.plugins.nbm.NbmPlugin

nbm {
  moduleName = 'com.foo.acme'
}
"""
        when:
        GradleProject project = runTasks(integTestDir, "nbm")

        then:
        // TODO expect output file with all required entries
        project != null
        project.tasks.find { it.name == 'nbm'} != null
        assertThat(new File(getIntegTestDir(), 'build/module/config/Modules/com-foo-acme.xml'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/modules/com-foo-acme.jar'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/update_tracking/com-foo-acme.xml'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/nbm/com-foo-acme.nbm'), FileMatchers.exists())
    }
}