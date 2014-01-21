package org.gradle.plugins.nbm;

import org.gradle.api.Project;
import org.gradle.api.Plugin;

public class NbmPlugin implements Plugin<Project> {
    void apply(Project project) {
        // Add the 'greeting' extension object
        // project.extensions.create("greeting", GreetingPluginExtension)
        // Add a task that uses the configuration
        project.task('hello') << {
            println 'hello' // project.greeting.message
        }
    }
}
