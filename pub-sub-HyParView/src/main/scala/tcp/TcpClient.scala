package tcp

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import java.net.InetSocketAddress

object Client {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[Client], remote, replies)
}

class Client(remote: InetSocketAddress, listener: ActorRef) extends Actor with ActorLogging{

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  log.info(s"remote = $remote")

  def receive = {
    case CommandFailed(_: Connect) ⇒
      log.info(s"Connection to $remote failed")
      listener ! "connect failed"
      context stop self

    case c@Connected(remote, local) ⇒
      listener ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case data: ByteString ⇒
          log.info(s"Sending = ${data.utf8String}")
          connection ! Write(data)
        case CommandFailed(w: Write) ⇒
          // O/S buffer was full
          listener ! "write failed"
        case Received(data) ⇒
          log.info(s"Receiving = $${data.utf8String}")
          listener ! data
        case "close" ⇒
          connection ! Close
        case _: ConnectionClosed ⇒
          listener ! "connection closed"
          context stop self
      }
  }
}

