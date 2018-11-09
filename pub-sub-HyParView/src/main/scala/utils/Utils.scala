package utils

import java.security.MessageDigest
import java.util.Date

import scala.util.Random

object Utils {


  def getDatePlusTime(TTL: Long) = {
    new Date(System.currentTimeMillis())
    //TODO
  }

  def getDate = {
    new Date()
  }

  def md5(s: String) = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }

  def pickRandomN[A](list: List[A], n: Int, elem: A = null) = {
    var auxList = list

    if (elem != null)
      auxList = list.filter(e => !e.equals(elem))

    Random.shuffle(auxList).take(n)
  }


}
