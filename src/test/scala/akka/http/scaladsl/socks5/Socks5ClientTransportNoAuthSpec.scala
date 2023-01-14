package akka.http.scaladsl.socks5

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import akka.stream.StreamTcpException
import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.wait.strategy.Wait

import java.net.InetSocketAddress

class Socks5ClientTransportNoAuthSpec extends AsyncFlatSpec with Matchers with ForAllTestContainer {

  override val container =
    GenericContainer(
      "xkuma/socks5:latest",
      exposedPorts = Seq(1080),
      waitStrategy = Wait.forListeningPort())

  implicit val system = ActorSystem()

  implicit val ec = system.dispatcher

  private def connectionSettings(proxyAddress: InetSocketAddress) = {
    val socks5ClientTransport = Socks5ClientTransport.socks5Proxy(proxyAddress)
    val clientConnectionSettings = ClientConnectionSettings(system).withTransport(socks5ClientTransport)
    ConnectionPoolSettings(system).withConnectionSettings(clientConnectionSettings)
  }

  it should "connect to github.com via proxy" in {
    val proxyAddress = InetSocketAddress.createUnresolved(container.containerIpAddress, container.mappedPort(1080))

    for {
      response <- Http().singleRequest(
        HttpRequest(uri = "http://github.com/"),
        settings = connectionSettings(proxyAddress))
    } yield {
      // should redirect to the HTTPS endpoint
      assert(response.status.intValue() == 301)
      assert(
        response.headers
          .find(_.lowercaseName() == "location")
          .map(_.value())
          .contains("https://github.com/"))
    }
  }

  it should "fail to connect to invalid proxy" in {
    val proxyAddress = InetSocketAddress.createUnresolved(container.containerIpAddress + "1234567", container.mappedPort(1080))

    for {
      _ <- recoverToSucceededIf[StreamTcpException](Http().singleRequest(
        HttpRequest(uri = "http://github.com/"),
        settings = connectionSettings(proxyAddress)))
    } yield {
      succeed
    }
  }

  it should "fail to connect to an invalid address via proxy" in {
    val proxyAddress = InetSocketAddress.createUnresolved(container.containerIpAddress, container.mappedPort(1080))

    for {
      _ <- recoverToSucceededIf[Socks5ConnectionException]( Http().singleRequest(
        HttpRequest(uri = s"http://${container.containerIpAddress}:${container.mappedPort(1080)}/"),
        settings = connectionSettings(proxyAddress)))
    } yield {
      succeed
    }
  }

}
