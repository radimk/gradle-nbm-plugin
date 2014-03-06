package org.gradle.plugins.nbm

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 *
 * @author radim
 */
class NbmPluginExtension {
    File harnessDir
    String moduleName
    String moduleVersion
    boolean eager
    boolean autoload

    private Configuration harnessConfiguration

    NbmPluginExtension(Project project) {
        harnessConfiguration = project.configurations.detachedConfiguration(
                project.dependencies.create('org.codehaus.mojo:nbm-maven-harness:7.4'))
    }

    Configuration getHarnessConfiguration() {
        harnessConfiguration
    }
}

