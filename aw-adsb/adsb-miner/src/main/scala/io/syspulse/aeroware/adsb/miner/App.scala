package io.syspulse.aeroware.adsb.miner

import java.time.Duration

import com.typesafe.scalalogging.Logger

import scopt.OParser

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util

import akka.NotUsed
import io.syspulse.aeroware.adsb.ingest.{ ADSB_Ingest }
import akka.stream.scaladsl.Flow
import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.ADSB_Event

case class ConfigArgs() {
  var c:Map[String,Any] = Map()
  def +(k:String,v:Any):ConfigArgs = {
    c = c + (k -> v)
    this
  }
  override def toString = s"${c}"
}

trait Arg[T]
case class ArgString(argChar:Char,argStr:String,argText:String,default:String="") extends Arg[String]()
case class ArgInt(argChar:Char,argStr:String,argText:String,default:Int=0) extends Arg[Int]()

class ConfigurationArgs(args:Array[String],ops: Arg[_]*) extends ConfigurationLike {
  val log = Logger(s"${this}")

  // def parseArgs(args:Array[String],ops: OParser[_,ConfigArgs]*) = {

  //   val builder = OParser.builder[ConfigArgs]
  //   val parser1 = {
  //     import builder._

  //     val options = List(
  //       head(Util.info._1, Util.info._2)
  //     ) ++ ops

  //     OParser.sequence(
  //       programName(Util.info._1), 
  //       options: _*
  //     )
  //   }

  def parseArgs(args:Array[String],ops: Arg[_]*) = {

    val builder = OParser.builder[ConfigArgs]
    val parser1 = {
      import builder._

      val options = List(
        head(Util.info._1, Util.info._2)
      ) ++ ops.flatMap(a => a match {
        case ArgString(c,s,t,d) => Some(opt[String](c, s).action((x, c) => c.+(s,x)).text(t))
        case ArgInt(c,s,t,d) => Some(opt[Int](c, s).action((x, c) => c.+(s,x)).text(t))
        case _ => None
      })

      OParser.sequence(
        programName(Util.info._1), 
        options: _*
      )
    }

    OParser.parse(parser1, args, ConfigArgs())
  }

  val configArgs = parseArgs(args,ops:_*)

  def getString(path:String):Option[String] = 
    if(!configArgs.isDefined) None else
    if (configArgs.get.c.contains(path)) configArgs.get.c.get(path).map(_.asInstanceOf[String]) else None
  
  def getInt(path:String):Option[Int] = 
    if(!configArgs.isDefined) None else
    if (configArgs.get.c.contains(path)) configArgs.get.c.get(path).map(_.asInstanceOf[Int]) else None

  def getLong(path:String):Option[Long] = 
    if(!configArgs.isDefined) None else
    if (configArgs.get.c.contains(path)) configArgs.get.c.get(path).map(_.asInstanceOf[Long]) else None

  def getAll():Seq[(String,Any)] = {
    if(!configArgs.isDefined) return Seq()

    configArgs.get.c.toSeq
  }

  def getDuration(path:String):Option[Duration] = 
    if(!configArgs.isDefined) None else
    if (configArgs.get.c.contains(path)) configArgs.get.c.get(path).map(v => Duration.ofMillis(v.asInstanceOf[Long])) else None
}

case class ADSB_HashEvent(hash:String,ts: Long, raw: String, `type`: String, icaoId:String, aircraft:String,callsign:String) extends ADSB_Event

object App extends skel.Server {

  // def parseArgs(args:Array[String], ops: OParser[_,ConfigArgs]*) = {

  //   val builder = OParser.builder[ConfigArgs]
  //   val parser1 = {
  //     import builder._

  //     val options = List(
  //       head(Util.info._1, Util.info._2),
  //       opt[String]('d', "dump1090.host").action((x, c) => c.+("host",x)).text("host")
  //     ) ++ ops

  //     OParser.sequence(
  //       programName(Util.info._1), 
  //       options: _*
  //     )
  //   }

  //   OParser.parse(parser1, args, ConfigArgs())
  // }


  def main(args: Array[String]):Unit = {

    println(s"args: ${args.size}: ${args.toSeq}")

    // val builder = OParser.builder[ConfigArgs]
    // import builder._
    // val configArgs = parseArgs(args, opt[String]('s', "sign").action((x, c) => c.+("sign",x)).text("Signing Key"))

    // configArgs match {
    //   case Some(configArgs) => {
  
        val configuration = Configuration.withPriority(Seq(
          new ConfigurationAkka,
          new ConfigurationProp,
          new ConfigurationEnv, 
          new ConfigurationArgs(args,
            ArgString('s', "sign","Signing Key"),
            ArgString('h', "dump1090.host","Dump1090 host"),
            ArgInt('p', "dump1090.port","Dump1090 port")
          )
        ))

        println(s"${configuration}")

        // (new ADSB_Ingest).run(io.syspulse.aeroware.adsb.ingest.Config(          
        //   dumpHost = "localhost",
        //   dumpPort = 30002,
        //   fileLimit = 1000000L
        // ))

        val config = io.syspulse.aeroware.adsb.ingest.Config(          
          dumpHost = configuration.getString("dump1090.host").getOrElse("localhost"),
          dumpPort = configuration.getInt("dump1090.port").getOrElse(30002),
          fileLimit = 1000000L,
          filePattern = "NONE"
        )

        println(config)

        (new ADSB_Ingest).run(config, 
          Some(Flow[ADSB].map(a =>{
            val hash = Util.SHA256(a.raw)
            ADSB_HashEvent(Util.hex(hash),
              a.ts, a.raw, a.getClass.getSimpleName,
              a.aircraftAddr.icaoId,
              a.aircraftAddr.icaoType,
              a.aircraftAddr.icaoCallsign) 
          })),
          Some((a:ADSB_Event) => {
            a.toString            
          })
        )

        //run( config.httpHost, config.httpPort, config.httpUri, configuration, Seq())
        
    //   }
    //   case _ =>
    //     System.exit(1)
    // }
  }
}
