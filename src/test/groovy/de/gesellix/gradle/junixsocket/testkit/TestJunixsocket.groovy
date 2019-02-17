package de.gesellix.gradle.junixsocket.testkit

import de.gesellix.docker.client.DockerClientImpl
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TestJunixsocket extends Specification {

  def "perform simple test"() {
    expect:
    200 == new DockerClientImpl().ping().status.code
  }

  def "perform async test"() {
    given:
    def result = new AtomicInteger()
    def latch = new CountDownLatch(1)

    when:
    Thread.start {
      def code = new DockerClientImpl().ping().status.code
      result.set(code)
      latch.countDown()
    }
    while (!latch.await(5, TimeUnit.SECONDS)) {
      Thread.sleep(500)
    }

    then:
    result.get() == 200
  }

  @Rule
  TemporaryFolder testProjectDir = new TemporaryFolder()

  def "perform gradle build"() {
    given:
    File buildFile = testProjectDir.newFile('build.gradle')
    buildFile << """
            buildscript {
              repositories {
                jcenter()
              }
            
              dependencies {
                classpath "de.gesellix:docker-client:2019-02-16T22-09-06"
                // work around https://github.com/kohlschutter/junixsocket/issues/59
                classpath('com.kohlschutter.junixsocket:junixsocket-native-common:2.1.1') {
                  force = true
                }
              }
            }

            task dockerPing() {
              doFirst {
                def result = new de.gesellix.docker.client.DockerClientImpl().ping()
                logger.lifecycle("request succeeded: " + (result.status.code == 200))
              }
            }
        """

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('dockerPing', '--debug', '--info', '--stacktrace')
        .withPluginClasspath()
        .build()

    then:
    result.output.contains("request succeeded: true")
    result.task(':dockerPing').outcome == TaskOutcome.SUCCESS
  }
}
