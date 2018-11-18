package membership

import utils.Node


object MetricsRequest

case class MetricsDelivery(layer : String, outgoingMessages : Int, incomingMessages : Int)