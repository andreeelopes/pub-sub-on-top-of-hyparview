package membershipproto

import java.security.MessageDigest

import scala.util.Random

object Utils {

  def md5(s: String) = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }

  def pickRandom[A](list: List[A]) = {
    list(Random.nextInt(list.size))
  }


}
