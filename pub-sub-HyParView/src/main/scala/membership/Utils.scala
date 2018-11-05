package membership

import java.security.MessageDigest

import scala.util.Random

object Utils {

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
