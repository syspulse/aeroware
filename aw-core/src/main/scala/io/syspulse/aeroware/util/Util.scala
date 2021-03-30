package io.syspulse.aeroware.util

import java.time._
import java.time.format._
import java.time.temporal._
import java.util.Locale
import io.jvm.uuid._

import java.security.SecureRandom
import java.nio.charset.StandardCharsets
import java.security.MessageDigest


object Util {

  val random = new SecureRandom
  val salt: Array[Byte] = Array.fill[Byte](16)(0x1f)
  val digest = MessageDigest.getInstance("SHA-256");  
  
  def toHexString(b:Array[Byte]) = b.foldLeft("")((s,b)=>s + f"$b%02x")

  def SHA256(data:Array[Byte]):Array[Byte] = digest.digest(data)
  def SHA256(data:String):Array[Byte] = digest.digest(data.getBytes(StandardCharsets.UTF_8))
  def sha256(data:Array[Byte]):String = toHexString(digest.digest(data))
  def sha256(data:String):String = toHexString(digest.digest(data.getBytes(StandardCharsets.UTF_8)))
  
  val tsFormatLong = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSS")

  def now:String = tsFormatLong.format(LocalDateTime.now)

  def info = {
    val p = getClass.getPackage
    val name = p.getImplementationTitle
    val version = p.getImplementationVersion
    (name,version)
  }

  def uuid(id:String,entityName:String=""):UUID = {
    val bb = Util.SHA256(entityName).take(4) ++  Array.fill[Byte](2+2+2)(0) ++ Util.SHA256(id).take(6)
    UUID(bb)
  }

}


