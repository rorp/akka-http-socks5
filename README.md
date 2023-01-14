# akka-http-socks5

An Akka HTTP transport that connects to target server via a SOCKS5 proxy.

### Use SOCKS5 proxy with Http().singleRequest

```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.socks5.Socks5ClientTransport
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}

import java.net.InetSocketAddress

implicit val system = ActorSystem()

val httpsProxyTransport = Socks5ClientTransport.socks5Proxy(InetSocketAddress.createUnresolved("localhost", 1080))

val settings = ConnectionPoolSettings(system).withTransport(httpsProxyTransport)

Http().singleRequest(HttpRequest(uri = "https://github.com"), settings = settings)
```

### Use SOCKS5 proxy that requres authentication

```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.socks5.Socks5ClientTransport
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}

import java.net.InetSocketAddress

implicit val system = ActorSystem()

val proxyAddress = InetSocketAddress.createUnresolved("localhost", 1080)

val proxyAuth = BasicHttpCredentials("proxy-user", "secret-proxy-pass")

val httpsProxyTransport = Socks5ClientTransport.socks5Proxy(proxyAddress, proxyAuth)

val settings = ConnectionPoolSettings(system).withTransport(httpsProxyTransport)

Http().singleRequest(HttpRequest(uri = "https://github.com"), settings = settings)
```
