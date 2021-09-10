package io.syspulse.aeroware.adsb.tools

import com.typesafe.scalalogging.Logger
import scala.collection.concurrent

import io.syspulse.skel
import io.syspulse.skel.config._

import io.syspulse.aeroware.adsb.core._

class PipeWebSocketServer(wsHost:String,wsPort:Int,interval:Long = 1L) extends cask.MainRoutes with PipePositionBaro {
  val logger = Logger(this.getClass.getSimpleName())

  override def host = wsHost
  override def port = wsPort

  def process(a0:ADSB_AirbornePositionBaro,a1:ADSB_AirbornePositionBaro) = {
    val loc = Decoder.getGloballPosition(a0,a1)
    if((a1.ts - aLast.get.ts) >= interval) {
      
      val data = output(a1,loc)
      broadcast(data)

      aLast = Some(a1)
    }
  }

  val clientsMap: concurrent.Map[cask.endpoints.WsChannelActor,String] = concurrent.TrieMap.empty

  def broadcast(data:String) = {
    clientsMap.foreach{ case(conn,id) =>
      conn.send(cask.Ws.Text(s"${data}"))
    }
  }
  def sendToClient(id: String, data:String) = {
    clientsMap.find{ case(clientId,conn) => id == clientId } match {
      case Some((conn,id)) => conn.send(cask.Ws.Text(s"${data}"))
      case None => logger.error(s"WS: Client ${id}: not found")
    }
  }

  @cask.websocket("/stream")
  def stream(): cask.WebsocketResult = {
    cask.WsHandler { connection =>
      cask.WsActor {
        case cask.Ws.Text("") => connection.send(cask.Ws.Close())
        case cask.Ws.Text(data) => {
          val id = data.trim
          logger.info(s"WS: ${connection}: <- id=[${id}]")
          clientsMap.put(connection,id)
          logger.debug(s"WS: ${clientsMap}")
        }
        case cask.Ws.Close(_, _) => {
          logger.info(s"WS: Close: ${connection}")
          clientsMap.remove(connection)
        }
        case _ => {
          logger.info(s"WS: ${connection}: ???")
          // assume disconennect and try to delete
          clientsMap.remove(connection)
          
        }
      }
    }
  }

  @cask.get("/")
  def info() = {
    this.getClass.getName()+"\n"
  }

  Console.err.println(s"Listening on ${host}:${port}...")
  initialize()
  main(Array())
}