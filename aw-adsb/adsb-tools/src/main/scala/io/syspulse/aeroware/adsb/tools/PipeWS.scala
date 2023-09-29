package io.syspulse.aeroware.adsb.tools

import com.typesafe.scalalogging.Logger
import scala.collection.concurrent

import io.syspulse.skel
import io.syspulse.skel.config._

import io.syspulse.aeroware.adsb.core._

abstract class PipeWS(wsHost:String,wsPort:Int) extends cask.MainRoutes {
  val logger = Logger(this.getClass.getSimpleName())

  override def host = wsHost
  override def port = wsPort

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

  @cask.websocket("/api/v1/radar/ws")
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