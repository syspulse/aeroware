package io.syspulse.aeroware.adsb.tools

import java.net.{ServerSocket, Socket}
import java.io.PrintWriter
import java.io.BufferedWriter
import java.io.OutputStreamWriter

import scala.util.Try
import com.typesafe.scalalogging.Logger
import scala.collection.concurrent

import io.syspulse.skel
import io.syspulse.skel.config._

import io.syspulse.aeroware.adsb.core._
import java.io.OutputStream
import java.net.InetAddress


class PipeTcpServer(tcpHost:String,tcpPort:Int,flush:Boolean = true) extends Thread with Pipe{
  val logger = Logger(this.getClass.getSimpleName())

  val clients: concurrent.Map[Socket,PrintWriter] = concurrent.TrieMap.empty

  override def run() : Unit = {
    val server = new ServerSocket(tcpPort,1000,InetAddress.getByName(tcpHost))
    @volatile var dead = false
    Console.err.println(s"Listening on ${tcpHost}:${tcpPort}...")
    while(!dead) {
      val sock = server.accept()
      logger.info(s"TCP(${tcpHost}:${tcpPort}) <- Connection(${sock})")
      val outStream = new PrintWriter(new BufferedWriter(new OutputStreamWriter(sock.getOutputStream)))
      clients.put(sock,outStream)
    }
  }

  def flow(a:Try[ADSB]):Try[ADSB] = {
    if(a.isSuccess && !a.get.isInstanceOf[ADSB_Unknown]) {
      broadcast(a.get.raw)
    }
    a
  }

  def broadcast(data:String) = {
    clients.foreach{ case(sock,outStream) =>
      outStream.println(data)
      if(flush) outStream.flush()
    }
  }
  def sendToClient(conn: Socket, data:String) = {
    clients.get(conn) match  {
      case Some(outStream) => { outStream.println(data); if(flush) outStream.flush()}
      case _ => logger.warn(s"Tcp client not found: ${conn}")
    }
  }

  start()
}