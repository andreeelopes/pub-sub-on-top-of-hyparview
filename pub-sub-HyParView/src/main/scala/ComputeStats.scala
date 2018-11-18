import java.io.{BufferedWriter, File, FileWriter}

import scala.io.Source

object ComputeStats {

  def main(args: Array[String]): Unit = {

    var subsByNode = Map[Int, List[Int]]() // [node1, List(topic1,...)]

    var publishByNode = Map[Int, List[(Int, Int)]]() // [node1, List((topic1, x),...)]
    var deliveredByNode = Map[Int, List[(Int, Int)]]() // [node1, List((topic1, x),...)]

    var publishTotal = Map[Int, Int]() //topic,count

    for (i <- 0 until 50) {
      subsByNode += (i -> List())
    }

    for (i <- 0 until 20) {
      publishByNode += (i -> List())
      deliveredByNode += (i -> List())
    }

    for (i <- 0 until 50) {
      publishTotal += (i -> 0)
    }


    for (line <- Source.fromFile("deploy/results/processedresults/results.csv").getLines) {
      val columns = line.split(",")

      val nodeId = columns(0).toInt

      columns(1) match {
        case "1" =>
          var topicList = subsByNode(nodeId)
          topicList ::= columns(2).toInt
          subsByNode = subsByNode.updated(nodeId, topicList)
        case "2" =>
          var topicCountList = publishByNode(nodeId)
          topicCountList ::= (columns(2).toInt, columns(3).toInt)
          publishByNode = publishByNode.updated(nodeId, topicCountList)
        case "3" =>
          var topicCountList = deliveredByNode(nodeId)
          topicCountList ::= (columns(2).toInt, columns(3).toInt)
          deliveredByNode = deliveredByNode.updated(nodeId, topicCountList)
      }

    }



    //Compute publications by topic of all system

    publishTotal = publishTotal.map { topicCount =>

      val topic = topicCount._1

      var nodesTopicCount = List[Int]()

      publishByNode.keys.foreach { key =>
        val topicCountList = publishByNode(key)

        if (topicCountList.nonEmpty) {
          val topic_count = topicCountList.filter(tc => tc._1.equals(topic))
          if (topic_count.nonEmpty) {
            nodesTopicCount ::= topic_count.head._2
          }
        }
      }
      (topic, nodesTopicCount.sum)
    }


    //    publishTotal.foreach(p => println(s"topic: ${p._1}, total: ${p._2}"))


    //accuracy by topic and node

    var accuracyByTopicAndNode = publishByNode.map { node_ListTopicCount =>

      val newList = node_ListTopicCount._2.map { topic_count =>

        val topicTotalCount = publishTotal(topic_count._1)

        var accuracy: Double = -1.0

        if (!subsByNode(node_ListTopicCount._1).contains(topic_count._1)) //not interested
          accuracy = -1.0
        else if (topicTotalCount.equals(0)) //no publications
          accuracy = 1.0
        else {
          val deliveredOfNodeList = deliveredByNode(node_ListTopicCount._1).filter(tc => tc._1.equals(topic_count._1))
          if (deliveredOfNodeList.isEmpty)
            accuracy = 0
          else
            accuracy = deliveredOfNodeList.head._2.toDouble / topicTotalCount.toDouble
        }

        (topic_count._1, accuracy)

      }

      (node_ListTopicCount._1, newList)
    }


    //    accuracyByTopicAndNode(0).foreach(p => println(s"topic: ${p._1}, accuracy: ${p._2}"))


    accuracyByTopicAndNode = accuracyByTopicAndNode.map { p =>

      val newList = p._2.filter(a => !a._2.equals(-1.0))
      (p._1, newList)
    }


    //    accuracyByTopicAndNode(1).foreach(p => println(s"topic: ${p._1}, accuracy: ${p._2}"))


    //accuracy by topic

    var accuracyByTopic = Map[Int, Double]()

    for (i <- 1 until 50) {

      var accuracyTopic = List[Double]()

      accuracyByTopicAndNode.keys.foreach { key =>

        val accuracyOfTopicOfNode = accuracyByTopicAndNode(key).filter(p => p._1.equals(i))

        if (accuracyOfTopicOfNode.nonEmpty)
          accuracyTopic ::= accuracyOfTopicOfNode.head._2
      }

      val accuracy =
        if (accuracyTopic.isEmpty)
          1
        else
          accuracyTopic.sum / accuracyTopic.size

      accuracyByTopic += (i -> accuracy)
    }

    //    accuracyByTopic.foreach(p => println(s"topic${p._1} accuracy ${p._2}"))


    //total accuracy

    var topicAccuracy = List[Double]()
    accuracyByTopic.foreach(p => topicAccuracy ::= p._2)

    val totalAccuracy = topicAccuracy.sum / topicAccuracy.size

    println(s"TotalAccuracy: $totalAccuracy")




    val file1 = new File("deploy/results/processedresults/totalAccuracy.csv")
    val pw1 = new BufferedWriter(new FileWriter(file1))
    pw1.write(s"${totalAccuracy.toString}\n")

    val file2 = new File("deploy/results/processedresults/accuracyByTopic.csv")
    val pw2 = new BufferedWriter(new FileWriter(file2))
    accuracyByTopic.foreach(p => pw2.write(s"${p._1},${p._2}\n"))


    val file3 = new File("deploy/results/processedresults/publishTotal.csv")
    val pw3 = new BufferedWriter(new FileWriter(file3))
    publishTotal.foreach(p => pw3.write(s"${p._1},${p._2}\n"))


    pw1.close()
    pw2.close()
    pw3.close()

  }


}