//
//import akka.actor.{ActorSystem, Props}
//import communication.CommunicationActor
//import membership.{HyParViewActor, StartLocal}
//import pubsub.{PubSubActor, Publish, Subscribe}
//import testapp.TestAppActor
//import utils.{Node, Start}
//
//
//object Local extends App {
//
//  override def main(args: Array[String]): Unit = {
//
//    val system = ActorSystem("system")
//
//    val testAppActorA = system.actorOf(Props[TestAppActor], "testAppA")
//    val pubSubActorA = system.actorOf(Props(new PubSubActor(20)), "pubSubA")
//    val communicationActorA = system.actorOf(Props(new CommunicationActor(3)), "communicationA")
//    val membershipActorA = system.actorOf(Props[HyParViewActor], "membershipA")
//    val nodeA = Node("A", testAppActorA, pubSubActorA, communicationActorA, membershipActorA)
//
//    val testAppActorB = system.actorOf(Props[TestAppActor], "testAppB")
//    val pubSubActorB = system.actorOf(Props(new PubSubActor(20)), "pubSubB")
//    val communicationActorB = system.actorOf(Props(new CommunicationActor(3)), "communicationB")
//    val membershipActorB = system.actorOf(Props[HyParViewActor], "membershipB")
//    val nodeB = Node("B", testAppActorB, pubSubActorB, communicationActorB, membershipActorB)
//
//    val testAppActorC = system.actorOf(Props[TestAppActor], "testAppC")
//    val pubSubActorC = system.actorOf(Props(new PubSubActor(20)), "pubSubC")
//    val communicationActorC = system.actorOf(Props(new CommunicationActor(3)), "communicationC")
//    val membershipActorC = system.actorOf(Props[HyParViewActor], "membershipC")
//    val nodeC = Node("C", testAppActorC, pubSubActorC, communicationActorC, membershipActorC)
//
//    val testAppActorD = system.actorOf(Props[TestAppActor], "testAppD")
//    val pubSubActorD = system.actorOf(Props(new PubSubActor(20)), "pubSubD")
//    val communicationActorD = system.actorOf(Props(new CommunicationActor(3)), "communicationD")
//    val membershipActorD = system.actorOf(Props[HyParViewActor], "membershipD")
//    val nodeD = Node("D", testAppActorD, pubSubActorD, communicationActorD, membershipActorD)
//
//    val testAppActorE = system.actorOf(Props[TestAppActor], "testAppE")
//    val pubSubActorE = system.actorOf(Props(new PubSubActor(20)), "pubSubE")
//    val communicationActorE = system.actorOf(Props(new CommunicationActor(3)), "communicationE")
//    val membershipActorE = system.actorOf(Props[HyParViewActor], "membershipE")
//    val nodeE = Node("E", testAppActorE, pubSubActorE, communicationActorE, membershipActorE)
//
//    val testAppActorF = system.actorOf(Props[TestAppActor], "testAppF")
//    val pubSubActorF = system.actorOf(Props(new PubSubActor(20)), "pubSubF")
//    val communicationActorF = system.actorOf(Props(new CommunicationActor(3)), "communicationF")
//    val membershipActorF = system.actorOf(Props[HyParViewActor], "membershipF")
//    val nodeF = Node("F", testAppActorF, pubSubActorF, communicationActorF, membershipActorF)
//
//
//    val testAppActorG = system.actorOf(Props[TestAppActor], "testAppG")
//    val pubSubActorG = system.actorOf(Props(new PubSubActor(20)), "pubSubG")
//    val communicationActorG = system.actorOf(Props(new CommunicationActor(3)), "communicationG")
//    val membershipActorG = system.actorOf(Props[HyParViewActor], "membershipG")
//    val nodeG = Node("G", testAppActorG, pubSubActorG, communicationActorG, membershipActorG)
//
//    val testAppActorH = system.actorOf(Props[TestAppActor], "testAppH")
//    val pubSubActorH = system.actorOf(Props(new PubSubActor(20)), "pubSubH")
//    val communicationActorH = system.actorOf(Props(new CommunicationActor(3)), "communicationH")
//    val membershipActorH = system.actorOf(Props[HyParViewActor], "membershipH")
//    val nodeH = Node("H", testAppActorH, pubSubActorH, communicationActorH, membershipActorH)
//
//    val testAppActorI = system.actorOf(Props[TestAppActor], "testAppI")
//    val pubSubActorI = system.actorOf(Props(new PubSubActor(20)), "pubSubI")
//    val communicationActorI = system.actorOf(Props(new CommunicationActor(3)), "communicationI")
//    val membershipActorI = system.actorOf(Props[HyParViewActor], "membershipI")
//    val nodeI = Node("I", testAppActorI, pubSubActorI, communicationActorI, membershipActorI)
//
//    val testAppActorJ = system.actorOf(Props[TestAppActor], "testAppJ")
//    val pubSubActorJ = system.actorOf(Props(new PubSubActor(20)), "pubSubJ")
//    val communicationActorJ = system.actorOf(Props(new CommunicationActor(3)), "communicationJ")
//    val membershipActorJ = system.actorOf(Props[HyParViewActor], "membershipJ")
//    val nodeJ = Node("J", testAppActorJ, pubSubActorJ, communicationActorJ, membershipActorJ)
//
//    val testAppActorL = system.actorOf(Props[TestAppActor], "testAppL")
//    val pubSubActorL = system.actorOf(Props(new PubSubActor(20)), "pubSubL")
//    val communicationActorL = system.actorOf(Props(new CommunicationActor(3)), "communicationL")
//    val membershipActorL = system.actorOf(Props[HyParViewActor], "membershipL")
//    val nodeL = Node("L", testAppActorL, pubSubActorL, communicationActorL, membershipActorL)
//
//
//    val testAppActorM = system.actorOf(Props[TestAppActor], "testAppM")
//    val pubSubActorM = system.actorOf(Props(new PubSubActor(20)), "pubSubM")
//    val communicationActorM = system.actorOf(Props(new CommunicationActor(3)), "communicationM")
//    val membershipActorM = system.actorOf(Props[HyParViewActor], "membershipM")
//    val nodeM = Node("M", testAppActorM, pubSubActorM, communicationActorM, membershipActorM)
//
//    val testAppActorN = system.actorOf(Props[TestAppActor], "testAppN")
//    val pubSubActorN = system.actorOf(Props(new PubSubActor(20)), "pubSubN")
//    val communicationActorN = system.actorOf(Props(new CommunicationActor(3)), "communicationN")
//    val membershipActorN = system.actorOf(Props[HyParViewActor], "membershipN")
//    val nodeN = Node("N", testAppActorN, pubSubActorN, communicationActorN, membershipActorN)
//
//
//    val testAppActorO = system.actorOf(Props[TestAppActor], "testAppO")
//    val pubSubActorO = system.actorOf(Props(new PubSubActor(20)), "pubSubO")
//    val communicationActorO = system.actorOf(Props(new CommunicationActor(3)), "communicationO")
//    val membershipActorO = system.actorOf(Props[HyParViewActor], "membershipO")
//    val nodeO = Node("O", testAppActorO, pubSubActorO, communicationActorO, membershipActorO)
//
//    val testAppActorP = system.actorOf(Props[TestAppActor], "testAppP")
//    val pubSubActorP = system.actorOf(Props(new PubSubActor(20)), "pubSubP")
//    val communicationActorP = system.actorOf(Props(new CommunicationActor(3)), "communicationP")
//    val membershipActorP = system.actorOf(Props[HyParViewActor], "membershipP")
//    val nodeP = Node("P", testAppActorP, pubSubActorP, communicationActorP, membershipActorP)
//
//    val testAppActorQ = system.actorOf(Props[TestAppActor], "testAppQ")
//    val pubSubActorQ = system.actorOf(Props(new PubSubActor(20)), "pubSubQ")
//    val communicationActorQ = system.actorOf(Props(new CommunicationActor(3)), "communicationQ")
//    val membershipActorQ = system.actorOf(Props[HyParViewActor], "membershipQ")
//    val nodeQ = Node("Q", testAppActorQ, pubSubActorQ, communicationActorQ, membershipActorQ)
//
//    val testAppActorR = system.actorOf(Props[TestAppActor], "testAppR")
//    val pubSubActorR = system.actorOf(Props(new PubSubActor(20)), "pubSubR")
//    val communicationActorR = system.actorOf(Props(new CommunicationActor(3)), "communicationR")
//    val membershipActorR = system.actorOf(Props[HyParViewActor], "membershipR")
//    val nodeR = Node("R", testAppActorR, pubSubActorR, communicationActorR, membershipActorR)
//
//    val testAppActorS = system.actorOf(Props[TestAppActor], "testAppS")
//    val pubSubActorS = system.actorOf(Props(new PubSubActor(20)), "pubSubS")
//    val communicationActorS = system.actorOf(Props(new CommunicationActor(3)), "communicationS")
//    val membershipActorS = system.actorOf(Props[HyParViewActor], "membershipS")
//    val nodeS = Node("S", testAppActorS, pubSubActorS, communicationActorS, membershipActorS)
//
//    val testAppActorT = system.actorOf(Props[TestAppActor], "testAppT")
//    val pubSubActorT = system.actorOf(Props(new PubSubActor(20)), "pubSubT")
//    val communicationActorT = system.actorOf(Props(new CommunicationActor(3)), "communicationT")
//    val membershipActorT = system.actorOf(Props[HyParViewActor], "membershipT")
//    val nodeT = Node("T", testAppActorT, pubSubActorT, communicationActorT, membershipActorT)
//
//
//    membershipActorA ! StartLocal(null, nodeA)
//    communicationActorA ! Start(nodeA)
//    pubSubActorA ! Start(nodeA)
//    testAppActorA ! Start(nodeA)
//
//    membershipActorB ! StartLocal(nodeA, nodeB)
//    communicationActorB ! Start(nodeB)
//    pubSubActorB ! Start(nodeB)
//    testAppActorB ! Start(nodeB)
//
//    membershipActorC ! StartLocal(nodeA, nodeC)
//    communicationActorC ! Start(nodeC)
//    pubSubActorC ! Start(nodeC)
//    testAppActorC ! Start(nodeC)
//
//    membershipActorD ! StartLocal(nodeA, nodeD)
//    communicationActorD ! Start(nodeD)
//    pubSubActorD ! Start(nodeD)
//    testAppActorD ! Start(nodeD)
//
//    membershipActorE ! StartLocal(nodeA, nodeE)
//    communicationActorE ! Start(nodeE)
//    pubSubActorE ! Start(nodeE)
//    testAppActorE ! Start(nodeE)
//
//    membershipActorF ! StartLocal(nodeA, nodeF)
//    communicationActorF ! Start(nodeF)
//    pubSubActorF ! Start(nodeF)
//    testAppActorF ! Start(nodeF)
//
//    membershipActorG ! StartLocal(nodeA, nodeG)
//    communicationActorG ! Start(nodeG)
//    pubSubActorG ! Start(nodeG)
//    testAppActorG ! Start(nodeG)
//
//    membershipActorH ! StartLocal(nodeA, nodeH)
//    communicationActorH ! Start(nodeH)
//    pubSubActorH ! Start(nodeH)
//    testAppActorH ! Start(nodeH)
//
//    membershipActorI ! StartLocal(nodeA, nodeI)
//    communicationActorI ! Start(nodeI)
//    pubSubActorI ! Start(nodeI)
//    testAppActorI ! Start(nodeI)
//
//    membershipActorJ ! StartLocal(nodeA, nodeJ)
//    communicationActorJ ! Start(nodeJ)
//    pubSubActorJ ! Start(nodeJ)
//    testAppActorJ ! Start(nodeJ)
//
//    membershipActorL ! StartLocal(nodeA, nodeL)
//    communicationActorL ! Start(nodeL)
//    pubSubActorL ! Start(nodeL)
//    testAppActorL ! Start(nodeL)
//
//    membershipActorM ! StartLocal(nodeA, nodeM)
//    communicationActorM ! Start(nodeM)
//    pubSubActorM ! Start(nodeM)
//    testAppActorM ! Start(nodeM)
//
//    membershipActorN ! StartLocal(nodeA, nodeN)
//    communicationActorN ! Start(nodeN)
//    pubSubActorN ! Start(nodeN)
//    testAppActorN ! Start(nodeN)
//
//    membershipActorO ! StartLocal(nodeA, nodeO)
//    communicationActorO ! Start(nodeO)
//    pubSubActorO ! Start(nodeO)
//    testAppActorO ! Start(nodeO)
//
//    membershipActorP ! StartLocal(nodeA, nodeP)
//    communicationActorP ! Start(nodeP)
//    pubSubActorP ! Start(nodeP)
//    testAppActorP ! Start(nodeP)
//
//    membershipActorQ ! StartLocal(nodeA, nodeQ)
//    communicationActorQ ! Start(nodeQ)
//    pubSubActorQ ! Start(nodeQ)
//    testAppActorQ ! Start(nodeQ)
//
//    membershipActorR ! StartLocal(nodeA, nodeR)
//    communicationActorR ! Start(nodeR)
//    pubSubActorR ! Start(nodeR)
//    testAppActorR ! Start(nodeR)
//
//    membershipActorS ! StartLocal(nodeA, nodeS)
//    communicationActorS ! Start(nodeS)
//    pubSubActorS ! Start(nodeS)
//    testAppActorS ! Start(nodeS)
//
//    membershipActorT ! StartLocal(nodeA, nodeT)
//    communicationActorT ! Start(nodeT)
//    pubSubActorT ! Start(nodeT)
//    testAppActorT ! Start(nodeT)
//
//
//
//    Thread.sleep(10000)
//
//    pubSubActorA ! Subscribe("futebol")
//    pubSubActorB ! Subscribe("futebol")
//    pubSubActorC ! Subscribe("futebol")
//    pubSubActorD ! Subscribe("futebol")
//    pubSubActorE ! Subscribe("futebol")
//    pubSubActorF ! Subscribe("futebol")
//    pubSubActorG ! Subscribe("futebol")
//    pubSubActorH ! Subscribe("futebol")
//    pubSubActorI ! Subscribe("futebol")
//    pubSubActorJ ! Subscribe("futebol")
//    pubSubActorL ! Subscribe("futebol")
//    pubSubActorM ! Subscribe("futebol")
//    pubSubActorN ! Subscribe("futebol")
//    pubSubActorO ! Subscribe("futebol")
//    pubSubActorP ! Subscribe("futebol")
//    pubSubActorQ ! Subscribe("futebol")
//    pubSubActorR ! Subscribe("futebol")
//    pubSubActorS ! Subscribe("futebol")
//    pubSubActorT ! Subscribe("futebol")
//
//    Thread.sleep(10000)
//
//    pubSubActorB ! Publish("futebol", "bruno carvalho")
//
//    Thread.sleep(10000)
//
//    system.terminate()
//
//
//  }
//
//
//}