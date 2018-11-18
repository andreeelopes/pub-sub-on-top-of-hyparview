package membership

import utils.Node


object CheckMetricsReceived

object MetricsRequest

case class MetricsDelivery(layer : String, outgoingMessages : Int, incomingMessages : Int)