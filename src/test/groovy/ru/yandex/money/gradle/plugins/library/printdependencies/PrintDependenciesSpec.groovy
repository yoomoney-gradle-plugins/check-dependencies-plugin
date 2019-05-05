package ru.yandex.money.gradle.plugins.library.printdependencies

import org.gradle.util.VersionNumber
import ru.yandex.money.gradle.plugins.library.AbstractPluginSpec


class PrintDependenciesSpec extends AbstractPluginSpec {

    def setup() {
        buildFile << """

                repositories {
                    maven { url 'http://nexus.yamoney.ru/content/repositories/thirdparty/' }
                    maven { url 'http://nexus.yamoney.ru/content/repositories/central/' }
                    maven { url 'http://nexus.yamoney.ru/content/repositories/releases/' }
                    maven { url 'http://nexus.yamoney.ru/content/repositories/public/' }
                }
                
                majorVersionChecker {
                    enabled = false
                }
        """.stripIndent()
    }

    def "Print new version for inner dependency"() {

        given:
        buildFile << """
                        
            dependencies {
                compile localGroovy(),
                        'ru.yandex.money.common:yamoney-json-utils:1.0.0',
                        'ru.yandex.money.common:yamoney-xml-utils:1.0.0',
                        'ru.yandex.money.common:yamoney-backend-platform-config:19.0.0',
                        'com.google.guava:guava:18.0'                       
            } 
            
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("printNewInnerDependenciesVersions")

        then:
        result.standardOutput.contains("ru.yandex.money.common:yamoney-json-utils 1.0.0 ->")
        result.standardOutput.contains("ru.yandex.money.common:yamoney-xml-utils 1.0.0 ->")
        !result.standardOutput.contains("com.google.guava:guava 22.0 ->")
        result.standardOutput.contains("ru.yandex.money.common:yamoney-backend-platform-config 19.0.0 ->")
        def update = result.standardOutput.findAll("ru\\.yandex\\.money\\.common:yamoney-backend-platform-config 19\\.0\\.0 -> (\\d+)\\.(\\d+)\\.(\\d+)")
        def semver = update[0] =~ /(\d+)\.(\d+)\.(\d+)/
        VersionNumber.parse(semver[1][0]) > VersionNumber.parse(semver[0][0])
    }

    def "Print new version for outer dependency"() {

        given:
        buildFile << """
            
            dependencies {
                compile localGroovy(),
                        'ru.yandex.money.common:yamoney-json-utils:1.0.0',
                        'com.google.guava:guava:22.0'
                       
            } 
          
                """.stripIndent()
        when:
        def result = runTasksSuccessfully("printNewOuterDependenciesVersions")

        then:
        result.standardOutput.contains("com.google.guava:guava 22.0 ->")
        !result.standardOutput.contains("ru.yandex.money.common:yamoney-json-utils 1.0.0 ->")
    }

    def "Print new version for outer and inner dependency"() {

        given:
        buildFile << """
            
            dependencies {
                compile 'ru.yandex.money.common:yamoney-json-utils:1.0.0',
                        'com.google.guava:guava:22.0'
            } 
            
                """.stripIndent()
        when:
        def result = runTasksSuccessfully("printNewOuterDependenciesVersions", "printNewInnerDependenciesVersions")

        then:
        result.standardOutput.contains("com.google.guava:guava 22.0 ->")
        result.standardOutput.contains("ru.yandex.money.common:yamoney-json-utils 1.0.0 ->")
    }

    def "Tasks show dependencies not execute when run 'build'"() {

        given:
        buildFile << """
            
            dependencies {
                compile 'ru.yandex.money.common:yamoney-json-utils:1.0.0',
                        'com.google.guava:guava:22.0'
                       
            } 
           
                """.stripIndent()
        when:
        def result = runTasksSuccessfully("build")

        then:
        !result.standardOutput.contains("com.google.guava:guava 22.0 ->")
        !result.standardOutput.contains("ru.yandex.money.common:yamoney-json-utils 1.0.0 ->")
    }

}