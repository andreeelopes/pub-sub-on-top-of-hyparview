package broadcast


case class Broadcast[E](mid: Array[Byte], message: E)

case class BCastDelivery[E](message: E)