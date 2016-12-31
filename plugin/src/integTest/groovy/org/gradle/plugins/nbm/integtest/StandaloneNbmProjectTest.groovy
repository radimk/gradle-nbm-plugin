package org.gradle.plugins.nbm.integtest

import com.google.common.base.Splitter
import com.google.common.collect.Iterables
import com.google.common.io.Files
import groovy.util.slurpersupport.GPathResult
import org.gradle.tooling.model.GradleProject

import java.util.jar.Attributes
import java.util.jar.JarFile
import org.apache.xml.resolver.CatalogManager
import org.apache.xml.resolver.tools.ResolvingXMLReader

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.not

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
        assertThat(new File(getIntegTestDir(), 'build/nbm/integTest.nbm'), FileMatchers.exists())
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

    def "build signed nbm"() {
        buildFile << """
apply plugin: 'java'
apply plugin: org.gradle.plugins.nbm.NbmPlugin

nbm {
  moduleName = 'com.foo.acme'
  keyStore {
    keyStoreFile = project.file('keystore')
    username = 'myself'
    password = 'specialsauce'
  }
}
"""
        when:
        Files.asByteSink(new File(getIntegTestDir(), 'keystore')).writeFrom(StandaloneNbmProjectTest.getResourceAsStream('keystore'))
        GradleProject project = runTasks(integTestDir, "nbm")

        then:
        project != null
        project.tasks.find { it.name == 'nbm'} != null
        assertThat(new File(getIntegTestDir(), 'build/nbm/com-foo-acme.nbm'), FileMatchers.exists())
    }

    def "build with module dependency"() {
        buildFile << """
apply plugin: 'java'
apply plugin: org.gradle.plugins.nbm.NbmPlugin

nbm {
  moduleName = 'com.foo.acme'
}
dependencies {
  compile 'org.netbeans.api:org-openide-awt:RELEASE74'
  compile 'org.netbeans.api:org-openide-util:RELEASE74'
}
"""
        def srcDir = createNewDir(integTestDir, 'src/main/java/com/mycompany/standalone')
        createNewFile(srcDir, 'Service.java') << """
package com.mycompany.standalone;
public interface Service {
    void action();
}
"""
        createNewFile(srcDir, 'ServiceImpl.java') << """
package com.mycompany.standalone;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = Service.class)
public class ServiceImpl implements Service {

    @Override public void action() {
        org.openide.util.Utilities.isUnix();
    }
}
"""
        createNewFile(srcDir, 'HelloAction.java') << """
package com.mycompany.standalone;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Help",
        id = "com.mycompany.standalone.HelloAction"
)
@ActionRegistration(
        displayName = "#CTL_HelloAction"
)
@ActionReference(path = "Menu/Help", position = 100)
@Messages("CTL_HelloAction=Say hello")
public final class HelloAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO implement action body
    }
}
"""
        def resDir = createNewDir(integTestDir, 'src/main/resources/com/mycompany/standalone')
        createNewFile(resDir, 'Bundle.properties') << """
MyKey=value
"""

        when:
        GradleProject project = runTasks(integTestDir, "netbeans")
        def moduleJar = new File(getIntegTestDir(), 'build/module/modules/com-foo-acme.jar')

        then:
        project != null
        project.tasks.find { it.name == 'nbm'} != null
        assertThat(new File(getIntegTestDir(), 'build/classes/main/META-INF/services/com.mycompany.standalone.Service'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/config/Modules/com-foo-acme.xml'), FileMatchers.exists())
        assertThat(moduleJar, FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/update_tracking/com-foo-acme.xml'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/.lastModified'), FileMatchers.exists())

        Iterables.contains(moduleDependencies(moduleJar), 'org.openide.util > 8.33.1')
        Iterables.contains(moduleDependencies(moduleJar), 'org.openide.awt > 7.59.1')
        moduleProperties(moduleJar, 'com/mycompany/standalone/Bundle.properties').getProperty('MyKey') == 'value'
        moduleProperties(moduleJar, 'com/mycompany/standalone/Bundle.properties').getProperty('CTL_HelloAction') == 'Say hello'
    }

    def "build with extra JAR"() {
        buildFile << """
apply plugin: 'java'
apply plugin: org.gradle.plugins.nbm.NbmPlugin

nbm {
  moduleName = 'com.foo.acme'
}
dependencies {
  compile 'org.netbeans.api:org-openide-util:RELEASE74'
  compile 'org.slf4j:slf4j-api:1.7.2'
}
"""
        def srcDir = createNewDir(integTestDir, 'src/main/java/com/mycompany/standalone')
        createNewFile(srcDir, 'Service.java') << """
package com.mycompany.standalone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class Service {
  public Service () {
    Logger logger = LoggerFactory.getLogger(Service.class);
  }
}
"""

        when:
        GradleProject project = runTasks(integTestDir, "netbeans")
        def moduleJar = new File(getIntegTestDir(), 'build/module/modules/com-foo-acme.jar')

        then:
        project != null
        project.tasks.find { it.name == 'nbm'} != null
        assertThat(moduleJar, FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/modules/ext/slf4j-api-1.7.2.jar'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/modules/ext/org-openide-util-lookup-RELEASE74.jar'), not(FileMatchers.exists()))

        Iterables.contains(moduleClasspath(moduleJar), 'ext/slf4j-api-1.7.2.jar')
    }

    def "build with no cluster defined"() {
        buildFile << """
apply plugin: 'java'
apply plugin: org.gradle.plugins.nbm.NbmPlugin

nbm {
  moduleName = 'com.foo.acme'
}
"""
        when:
        GradleProject project = runTasks(integTestDir, "nbm")
        File module = new File(getIntegTestDir(), 'build/nbm/com-foo-acme.nbm')
		
        then:
        // TODO expect output file with all required entries
        project != null
        project.tasks.find { it.name == 'nbm'} != null
        assertThat(new File(getIntegTestDir(), 'build/module/config/Modules/com-foo-acme.xml'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/modules/com-foo-acme.jar'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/update_tracking/com-foo-acme.xml'), FileMatchers.exists())	
        assertThat(module, FileMatchers.exists())
        moduleXml(module, 'Info/info.xml').getProperty('@targetcluster').text().isEmpty()
    }
	
    def "build with cluster defined that is not called 'extra'"() {
        buildFile << """
apply plugin: 'java'
apply plugin: org.gradle.plugins.nbm.NbmPlugin

nbm {
  cluster = 'myCluster'
  moduleName = 'com.foo.acme'
}
"""
        when:
        GradleProject project = runTasks(integTestDir, "nbm")
        File module = new File(getIntegTestDir(), 'build/nbm/com-foo-acme.nbm')
		
        then:
        // TODO expect output file with all required entries
        project != null
        project.tasks.find { it.name == 'nbm'} != null
        assertThat(new File(getIntegTestDir(), 'build/module/config/Modules/com-foo-acme.xml'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/modules/com-foo-acme.jar'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/update_tracking/com-foo-acme.xml'), FileMatchers.exists())	
        assertThat(module, FileMatchers.exists())
        moduleXml(module, 'Info/info.xml').getProperty('@targetcluster').text() == "myCluster"
    }
	
    def "build with cluster defined that is called 'extra'"() {
        buildFile << """
apply plugin: 'java'
apply plugin: org.gradle.plugins.nbm.NbmPlugin

nbm {
  cluster = 'extra'
  moduleName = 'com.foo.acme'
}
"""
        when:
        GradleProject project = runTasks(integTestDir, "nbm")
        File module = new File(getIntegTestDir(), 'build/nbm/com-foo-acme.nbm')
		
        then:
        // TODO expect output file with all required entries
        project != null
        project.tasks.find { it.name == 'nbm'} != null
        assertThat(new File(getIntegTestDir(), 'build/module/config/Modules/com-foo-acme.xml'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/modules/com-foo-acme.jar'), FileMatchers.exists())
        assertThat(new File(getIntegTestDir(), 'build/module/update_tracking/com-foo-acme.xml'), FileMatchers.exists())	
        assertThat(module, FileMatchers.exists())
        moduleXml(module, 'Info/info.xml').getProperty('@targetcluster').text().isEmpty()
    }
	
    private Iterable<String> moduleDependencies(File jarFile) {
        JarFile jar = new JarFile(jarFile)
        def attrs = jar.manifest?.mainAttributes
        def attrValue = attrs?.getValue(new Attributes.Name('OpenIDE-Module-Module-Dependencies'))
        jar.close()
        Splitter.on(',').trimResults().split(attrValue != null ? attrValue : '')
    }

    private Iterable<String> moduleClasspath(File jarFile) {
        JarFile jar = new JarFile(jarFile)
        def attrs = jar.manifest?.mainAttributes
        def attrValue = attrs.getValue(new Attributes.Name('Class-Path'))
        jar.close()
        Splitter.on(',').trimResults().split(attrValue != null ? attrValue : '')
    }

    private Properties moduleProperties(File jarFile, String resourceName) {
        JarFile jar = new JarFile(jarFile)
        def is = jar.getInputStream(jar.getEntry(resourceName))
        def props = new Properties()
        props.load(is)
        is.close()
        jar.close()
        props
    }
	
    private GPathResult moduleXml(File jarFile, String resourceName) {
        new JarFile(jarFile).withCloseable { jar ->
            jar.getInputStream(jar.getEntry(resourceName)).withCloseable { is ->
                return new XmlSlurper(new ResolvingXMLReader(cm)).parse(is)
            }
        }
    }
}