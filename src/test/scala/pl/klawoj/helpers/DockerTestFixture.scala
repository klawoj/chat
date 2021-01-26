package pl.klawoj.helpers

import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.model._
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientBuilder}

import java.nio.file.Path
import java.util.{List => JavaList}
import scala.collection.JavaConverters._
import scala.util.Try


trait DockerTestFixture {
  private val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
  private val docker = DockerClientBuilder.getInstance(dockerConfig).build

  case class DockerFixtureException(message: String) extends Exception

  def createContainer(image: String, name: String, port: Int, env: List[String] = Nil, mounts: Map[Path, Path] = Map.empty): CreateContainerResponse = {
    val exposedPort = ExposedPort.tcp(port)
    val portBindings = new Ports
    portBindings.bind(exposedPort, Ports.Binding.bindPort(port))

    val binds = mounts.map(b => new Bind(b._1.toString, new Volume(b._2.toString))).toList
    docker.createContainerCmd(image).withPortBindings(portBindings).withBinds().withName(name).withBinds(binds.asJava).withEnv(env.asJava).exec()
  }

  def findContainerIdByName(name: String): String =
    findContainerIdByNameOption(name).getOrElse(throw DockerFixtureException(s"container with name '$name' not found"))

  def findContainerIdByNameOption(name: String): Option[String] = {
    val response: JavaList[Container] = docker.listContainersCmd().withShowAll(true).exec()
    response.asScala.find(_.getNames.contains(name)).map(_.getId)
  }

  def startContainer(name: String): Unit = {
    val id = findContainerIdByName(name)
    docker.startContainerCmd(id).exec()
  }

  def removeContainer(name: String): Unit = {
    findContainerIdByNameOption(name).foreach { id =>
      Try(docker.stopContainerCmd(id).exec())
      Try(docker.removeContainerCmd(id).exec())
    }
  }
}
