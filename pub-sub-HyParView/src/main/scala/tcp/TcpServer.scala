package tcp

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import java.net.InetSocketAddress

class TcpServer extends Actor with ActorLogging {

  import akka.io.Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 69))

  def receive = {
    case b@Bound(localAddress) ⇒
      log.info("Server listening at = " + localAddress)
      context.parent ! b

    case CommandFailed(_: Bind) ⇒
      log.info("Connection binding failed")
      context stop self

    case c@Connected(remote, local) ⇒
      log.info("Connection received from hostname: " + remote.getHostName + " address: " + remote.getAddress)
      val handler = context.actorOf(Props[SimplisticHandler])
      val connection = sender()
      connection ! Register(handler)
  }

}

class SimplisticHandler extends Actor with ActorLogging {

  import Tcp._

  def receive = {
    case Received(data) ⇒
      log.info(s"Received this data = ${data.utf8String}")
      sender() ! Write(data)
    case WriteTcpMessage(t) =>
      log.info(s"Received custom TCP message = $t")
    case PeerClosed ⇒
      context stop self
    case _: ConnectionClosed ⇒
      log.info(s"Connection dropped")
  }
}