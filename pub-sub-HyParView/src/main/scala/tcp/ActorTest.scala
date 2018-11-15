package tcp


import akka.actor.Actor
import akka.util.ByteString


class ActorTest extends Actor {
  def receive: Receive = {
    case data : ByteString =>
      println(s"DEBUG = $data")
  }

}
