package utils

import java.security.MessageDigest
import java.util.{Calendar}

import scala.util.Random

object Utils {


  def getDatePlusTime(TTL: Int) = {
    val now = Calendar.getInstance()
    now.add(Calendar.SECOND, TTL)
    now.getTime
  }


  def getDate = {
    Calendar.getInstance().getTime
  }

  def md5(s: String) = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }

  def pickRandomN[A](list: List[A], n: Int, except: A = null) = {
    Random.shuffle(list.filter(p => !p.equals(except))).take(n)
  }
}
