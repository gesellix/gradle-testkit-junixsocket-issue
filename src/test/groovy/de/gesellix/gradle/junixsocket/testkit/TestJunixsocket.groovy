package de.gesellix.gradle.junixsocket.testkit

import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Slf4j
class TestJunixsocket extends Specification {

  def "perform simple test"() {
    expect:
    AFUNIXSocketCheck.supported()
  }

  def "perform async test"() {
    given:
    def result = new AtomicBoolean()
    def latch = new CountDownLatch(1)

    when:
    Thread.start {
      boolean supported = AFUNIXSocketCheck.supported()
      result.set(supported)
      latch.countDown()
    }
    while (!latch.await(5, TimeUnit.SECONDS)) {
      Thread.sleep(500)
    }

    then:
    result.get()
  }

  @Rule
  TemporaryFolder testProjectDir = new TemporaryFolder()

  def "perform gradle build"() {
    given:
    File buildFile = testProjectDir.newFile('build.gradle')
    buildFile << """
            import org.newsclub.net.unix.AFUNIXSocket

            buildscript {
              repositories {
                jcenter()
              }
            
              dependencies {
                classpath('com.kohlschutter.junixsocket:junixsocket-core:2.2.0')
                classpath('com.kohlschutter.junixsocket:junixsocket-common:2.2.0')
              }
            }

            task checkAFUNIXSupport() {
              doFirst {
                boolean supported = AFUNIXSocket.isSupported()
                logger.lifecycle("AFUNIXSocket.isSupported(): \${supported}")
              }
              doLast {
                AFUNIXSocket socket = AFUNIXSocket.newInstance()
                logger.lifecycle("AFUNIXSocket.newInstance(): \${socket}")
              }
            }
        """

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir.root)
        .withArguments('checkAFUNIXSupport', '--debug', '--info', '--stacktrace')
        .withPluginClasspath()
        .build()

    then:
    result.task(':checkAFUNIXSupport').outcome == TaskOutcome.SUCCESS
  }
}
