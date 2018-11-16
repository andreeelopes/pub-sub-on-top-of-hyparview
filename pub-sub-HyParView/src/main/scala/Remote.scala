import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import gossip.GossipActor
import membership.HyParViewActor
import pubsub.PubSubActor
import testapp.TestAppActor
import utils.{Node, Start}

object Remote extends App {

  //30 topicos
  //cada nó subscreve 5 topicos de 50 fixos
  // 5 topicos publish ao calhas de 1/2s   publish(Ti, myIp:myPort:seq) seq => sequencia da mensagem
  // registar em cada nó os que subscreveu, os que publicou
  //verificar se os tópicos menos populares sejam mais entregues

  //quantas mensagens de rede estamos a enviar, ao nível do pubsub, em média cada processo mandou x mensagens

  //testar com falhas durante

  /*
  Introdução -> o que foi feito
  overview -> solução, componentes
  detailed overview ->
        3 subsecções: overlay, pubsub, test
  pseudo-code e argumentos de correção:
      overlay, pubsub
  avaliação experimental
      experiencia: (nº de máquinas, nº de processos, 5 subs, 5 pubs)
      resultados e discussão
  conclusão
   */

  override def main(args: Array[String]) = {

    val ip = args(0)
    val port = args(1)

    val config = ConfigFactory.parseString(getConf(ip, port))

    val system = ActorSystem("RemoteService", config)

    val testAppActor = system.actorOf(Props[TestAppActor], "testApp")
    val pubSubActor = system.actorOf(Props(new PubSubActor(2)), "pubSub")
    val gossipActor = system.actorOf(Props(new GossipActor(3)), "gossip")
    val membershipActor = system.actorOf(Props[HyParViewActor], "membership")

    val node = Node(s"$ip:$port", testAppActor, pubSubActor, gossipActor, membershipActor)
    println("MyNode: " + node)

    var contactAkkaId: String = null

    try {
      val ipContactNode = args(2)
      val portContactNode = args(3)
      contactAkkaId = s"akka.tcp://RemoteService@$ipContactNode:$portContactNode/user/membership"
    } catch {
      case _: Exception =>
    }


    testAppActor ! Start(node)
    pubSubActor ! Start(node)
    gossipActor ! Start(node)
    membershipActor ! membership.Start(contactAkkaId, node)

  }

  def getConf(ip: String, port: String) = {
    s"""
       |akka {
       |   actor {
       |     provider = "remote"
       |     warn-about-java-serializer-usage = false
       |   }
       |   remote {
       |     enabled-transports = ["akka.remote.netty.tcp"]
       |     netty.tcp {
       |       hostname = "$ip"
       |       port = $port
       |     }
       |   }
       |  }
    """.stripMargin
  }


}
