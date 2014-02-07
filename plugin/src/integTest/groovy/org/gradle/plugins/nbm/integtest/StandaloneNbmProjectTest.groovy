package org.gradle.plugins.nbm.integtest

import org.gradle.tooling.model.GradleProject

class StandaloneNbmProjectTest extends AbstractIntegrationTest {
    def setup() {
        buildFile << """
apply plugin: 'java'
apply plugin: org.gradle.plugins.nbm.NbmPlugin

"""
    }

    def "load project"() {
        when:
        GradleProject project = runTasks(integTestDir, "tasks")

        then:
        project != null
        project.tasks.find { it.name == 'nbm'} != null
    }

    def "run nbm"() {
        when:
        GradleProject project = runTasks(integTestDir, "nbm")

        then:
        // TODO expect output file with all required entries
        project != null
        project.tasks.find { it.name == 'nbm'} != null
    }
}