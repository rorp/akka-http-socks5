package io.github.rorp.akka.http.scaladsl.socks5

import akka.actor.ActorSystem
import akka.http.scaladsl.ClientTransport.TCP
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.http.scaladsl.{ClientTransport, Http}
import akka.stream.scaladsl.{Flow, Keep}
import akka.util.ByteString

import java.net.InetSocketAddress
import scala.concurrent.Future

/**
 * Simple SOCKS5 client transport
 *
 * Created by rorp
 */
private [socks5] case class Socks5ClientTransport(proxyAddress: InetSocketAddress, proxyCredentials: Option[BasicHttpCredentials])
  extends ClientTransport {

  override def connectTo(
                          host: String,
                          port: Int,
                          settings: ClientConnectionSettings)(implicit system: ActorSystem): Flow[
    ByteString,
    ByteString,
    Future[Http.OutgoingConnection]] = {
    Socks5ProxyGraphStage(host, port, proxyAddress, proxyCredentials)
      .joinMat(
        TCP.connectTo(proxyAddress.getHostString,
          proxyAddress.getPort,
          settings))(Keep.right)
      .mapMaterializedValue(_.map(_.copy(remoteAddress =
        InetSocketAddress.createUnresolved(host, port)))(system.dispatcher))
  }
}

object Socks5ClientTransport {
  def socks5Proxy(proxyAddress: InetSocketAddress): ClientTransport =
    Socks5ClientTransport(proxyAddress, None)

  def socks5Proxy(proxyAddress: InetSocketAddress, proxyCredentials: BasicHttpCredentials): ClientTransport =
    Socks5ClientTransport(proxyAddress, Some(proxyCredentials))
}

