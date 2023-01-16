package io.github.rorp.akka.http.scaladsl.socks5

import akka.util.ByteString

import java.io.IOException
import java.net.{Inet4Address, Inet6Address, InetAddress, InetSocketAddress}
import java.nio.ByteBuffer
import scala.util.{Failure, Success, Try}

/**
 * SOCKS5 protocol primitives
 *
 * Created by rorp
 */
case class Socks5ConnectionException(str: String) extends IOException

private[socks5] object Socks5Protocol {
  val NoAuth: Byte = 0x00
  val PasswordAuth: Byte = 0x02
  val connectErrors: Map[Byte, String] = Map[Byte, String](
    (0x00, "Request granted"),
    (0x01, "General failure"),
    (0x02, "Connection not allowed by ruleset"),
    (0x03, "Network unreachable"),
    (0x04, "Host unreachable"),
    (0x05, "Connection refused by destination host"),
    (0x06, "TTL expired"),
    (0x07, "Command not supported / protocol error"),
    (0x08, "Address type not supported")
  )

  def socks5Greeting(passwordAuth: Boolean): ByteString = ByteString(
    0x05, // SOCKS version
    0x01, // number of authentication methods supported
    if (passwordAuth) PasswordAuth else NoAuth
  ) // auth method

  def socks5PasswordAuthenticationRequest(
                                           username: String,
                                           password: String): ByteString = {
    val usernameBytes = ByteString(username)
    val passwordBytes = ByteString(password)
    ByteString(0x01, // version of username/password authentication
      usernameBytes.length.toByte) ++
      usernameBytes ++
      ByteString(passwordBytes.length.toByte) ++
      passwordBytes
  }

  def socks5ConnectionRequest(address: InetSocketAddress): ByteString = {
    ByteString(0x05, // SOCKS version
      0x01, // establish a TCP/IP stream connection
      0x00) ++ // reserved
      addressToByteString(address) ++
      portToByteString(address.getPort)
  }

  def addressToByteString(address: InetSocketAddress): ByteString =
    if (address.isUnresolved) {
      // unresolved address, use SOCKS5 resolver
      val host = address.getHostString
      ByteString(0x03, // Domain name
        host.length.toByte) ++
        ByteString(host)
    } else {
      inetAddressToByteString(address.getAddress)
    }

  def inetAddressToByteString(inet: InetAddress): ByteString = inet match {
    case a: Inet4Address =>
      ByteString(
        0x01 // IPv4 address
      ) ++ ByteString(a.getAddress)
    case a: Inet6Address =>
      ByteString(
        0x04 // IPv6 address
      ) ++ ByteString(a.getAddress)
    case _ => throw Socks5ConnectionException("Unknown InetAddress")
  }

  def portToByteString(port: Int): ByteString =
    ByteString((port & 0x0000ff00) >> 8, port & 0x000000ff)

  def tryParseConnectedAddress(data: ByteString): Try[InetSocketAddress] = {
    if (data(0) != 0x05) {
      throw Socks5ConnectionException("Invalid proxy version")
    } else {
      val status = data(1)
      if (status != 0) {
        Failure(socks5Exception(status))
      } else {
        data(3) match {
          case 0x01 =>
            val ip = Array(data(4), data(5), data(6), data(7))
            val port = (data(8) & 0xff) << 8 | data(9) & 0xff
            val socket =
              new InetSocketAddress(InetAddress.getByAddress(ip), port)
            Success(socket)
          case 0x03 =>
            val len = data(4)
            val start = 5
            val end = start + len
            val domain = data.slice(start, end).utf8String
            val port = (data(end) & 0xff) << 8 | (data(end + 1) & 0xff)
            val socket = new InetSocketAddress(domain, port)
            Success(socket)
          case 0x04 =>
            val ip = Array.ofDim[Byte](16)
            data.copyToArray(ip, 4, 4 + ip.length)
            val port = (data(4 + ip.length) & 0xff) << 8 | (data(4 + ip.length + 1) & 0xff)
            val socket =
              new InetSocketAddress(InetAddress.getByAddress(ip), port)
            Success(socket)
          case b => Failure(Socks5ConnectionException(s"Unrecognized address type $b"))
        }
      }
    }
  }

  def socks5Exception(code: Byte): IOException =
    Socks5ConnectionException(connectErrors.getOrElse(code, s"SOCKS5 error: unknown error code $code"))

  def tryParseGreetings(data: ByteString, passwordAuth: Boolean): Try[Byte] =
    Try(parseGreetings(data, passwordAuth))

  def parseGreetings(data: ByteString, passwordAuth: Boolean): Byte = {
    if (data(0) != 0x05) {
      throw Socks5ConnectionException("Invalid SOCKS5 version")
    } else if (
      (!passwordAuth && data(1) != NoAuth) || (passwordAuth && data(
        1) != PasswordAuth)
    ) {
      throw Socks5ConnectionException("Unsupported SOCKS5 auth method")
    } else {
      data(1)
    }
  }

  def tryParseAuth(data: ByteString): Try[Boolean] = Try(parseAuth(data))

  def parseAuth(data: ByteString): Boolean = {
    if (data(0) != 0x01) {
      throw Socks5ConnectionException("Invalid SOCKS5 auth method")
    } else if (data(1) != 0) {
      throw Socks5ConnectionException("SOCKS5 authentication failed")
    } else {
      true
    }
  }
}

