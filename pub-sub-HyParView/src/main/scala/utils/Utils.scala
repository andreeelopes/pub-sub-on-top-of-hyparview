/*
package utils

import java.security.MessageDigest
import java.util.{Calendar, Date}

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

  def pickRandomN[A](list: List[A], n: Int) = {
    Random.shuffle(list).take(n)
  }

  def serialise(value: Any): Array[Byte] = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(value)
    oos.close()
    stream.toByteArray
  }

  def deserialise(bytes: Array[Byte]): Any = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(bytes))
    val value = ois.readObject
    ois.close()
    value
  }


}
*/
