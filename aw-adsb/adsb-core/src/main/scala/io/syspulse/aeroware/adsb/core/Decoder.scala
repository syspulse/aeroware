package io.syspulse.aeroware.adsb.core

import scala.util.{Try,Success,Failure}

import scodec.codecs._
import scodec.bits._
import scodec.Codec

// import java.nio.file.{Paths, Files}
// import akka.stream._
// import akka.stream.scaladsl._
// import akka.util.ByteString
// import scala.concurrent.duration._
// import scala.concurrent.Await

// import akka.actor.ActorSystem
// implicit val system = ActorSystem("ADSB")

// import java.time.ZoneId
// import java.time.ZonedDateTime
// import java.time.Instant
// import java.time.format._

// import java.net.InetSocketAddress

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.util._
import io.syspulse.aeroware.util._

case class RawAirbornePosition(SS: BitVector,NICsb: BitVector,ALT: BitVector,T: BitVector,F: BitVector,
  LAT_CPR: BitVector,LON_CPR: BitVector) {
  override def toString = {
    s"RawAirbornePosition(SS=${SS.toByte()},NICsb=${NICsb.toBin},ALT=${ALT
      .toLong()},T=${T.toBin} F=${F.toBin},LAT_CPR=${LAT_CPR
      .toLong()},LON_CPR=${LON_CPR.toLong()})"
  }
}

case class RawAircraftIdentification(EC: BitVector,C1:BitVector,C2:BitVector,C3:BitVector,C4:BitVector,C5:BitVector,C6:BitVector,C7:BitVector,C8:BitVector) {
  override def toString = {
    s"RawAircraftIdentification(EC=${EC.toByte()},C1=${C1.toBin},C2=${C2.toBin},C3=${C3.toBin},C4=${C4.toBin},C5=${C5.toBin},C6=${C6.toBin},C7=${C7.toBin},C8=${C8.toBin})"
  }
}

case class RawADSB(DF: BitVector, CA: BitVector, ICAO: BitVector, TC: BitVector, DATA: BitVector, PI: BitVector) {
  override def toString = {
    val tc = TC.toByte(true)
    val tcInfo = tc match {
      case v if 1 until 5 contains v => "Aircraft Idntification"
      case v if 5 until 9 contains v => "Surface Position"
      case v if 9 until 19 contains v =>
        (
          "Airborne position (w/ Baro Altitude)",
          Decoder.codecRawAirbornePositions.decode(DATA).toOption.get.value
        )
      case 19                          => "Airborne velocities"
      case v if 20 until 23 contains v => "Airborne position (w/ GNSS Height)"
      case v if 23 until 28 contains v => "Reserved"
      case 28                          => "Aircraft status"
      case 29                          => "Target state and status information"
      case 31                          => "Aircraft operation status"
      case _                           => "Unknown"
    }

    s"RawADSB(DF=${DF.toBin},CA=${CA.toBin},ICAO=${ICAO.toHex},TC=${TC.toByte()} ($tcInfo), DATA=${DATA.toHex},PI=${PI.toHex})"
  }
}

abstract class Decoder {
  val log = Logger(this.getClass().getSimpleName())
  
  def decodeAircraftAddr(b:BitVector):AircraftAddress = {
    val icaoId = b.toHex.toLowerCase
    val aircraft = AircraftICAORegistry.find(icaoId)
    val (icaoType,icaoCallsign) = if(aircraft.isDefined) (aircraft.get.icaoType,aircraft.get.regid) else ("","")
        AircraftAddress(icaoId,icaoType,icaoCallsign)    
  }


  def decode(data: String): Try[ADSB] = {
    val message = data.trim
    if(message.size == 0 || message.size < 14 || message.size > 28 ) 
      return Failure(new Exception(s"invalid size: ${message.size}"))

    val df = try {
      Decoder.codecRawDF.decode(BitVector.fromHex(message).get).toOption.get.value.toByte(false)
    } catch {
      case e:Exception => return Failure(new Exception(s"invalid format: failed to parse DF: ${data}"))
    }
    
    val now = System.currentTimeMillis

    log.trace(s"msg=${message}: DF=${df}")

    val adsb = df match {
      case 17 | 18 | 19 =>
        val raw = Decoder.codecRawADSB.decode(BitVector.fromHex(message).get).toOption.get.value

        val df = raw.DF.toByte(false)
        val capability = raw.CA.toByte(false)
        val aircraftAddr = decodeAircraftAddr(raw.ICAO)

        val tc = raw.TC.toByte(false)
        tc match {
          case v if 1 until 5 contains v => {
            val aid = Decoder.codecRawAircraftIdentification.decode(raw.DATA).toOption.get.value
            ADSB_AircraftIdentification(df,capability,aircraftAddr,
              tc, aid.EC.toByte(false),
              callSign = Decoder.decodeDataAsChars(Seq(aid.C1,aid.C2,aid.C3,aid.C4,aid.C5,aid.C6,aid.C7,aid.C8)),
              raw = message)
          }
          case v if 5 until 9 contains v => ADSB_SurfacePosition(df,capability,aircraftAddr,raw = message)
          case v if 9 until 19 contains v =>
            (
              "Airborne position (w/ Baro Altitude)",
              Decoder.codecRawAirbornePositions.decode(raw.DATA).toOption.get.value
            )
            ADSB_AirbornePositionBaro(df,capability,aircraftAddr,raw = message)
          case 19                          => ADSB_AirborneVelocity(df,capability,aircraftAddr,raw = message)
          case v if 20 until 23 contains v => ADSB_AirbornePositionGNSS(df,capability,aircraftAddr,raw = message)
          case v if 23 until 28 contains v => ADSB_Reserved(df,capability,aircraftAddr,raw = message)
          case 28                          => ADSB_AircraftStatus(df,capability,aircraftAddr,raw = message)
          case 29                          => ADSB_TargetState(df,capability,aircraftAddr,raw = message)
          case 31                          => ADSB_AircraftOperationStatus(df,capability,aircraftAddr,raw = message)
          case _                           => ADSB_Unknown(df,capability,aircraftAddr,raw = message)
        }
      // case 18 => // non-interrogatable equipment
      //   //log.warn(s"msg=${message}: DF=${df}: Unsupported DF")
      //   ADSB_Unknown(df,0,AircraftAddress("0","",""),raw = message)
      case _ => // unknown
        //log.warn(s"msg=${message}: DF=${df}: Unsupported DF")
        ADSB_Unknown(df,0,AircraftAddress("0","",""),raw = message)
    }
    Success(adsb)
  }

  def decodeAirbornePosition(message: String): RawAirbornePosition =
    Decoder.codecRawAirbornePositions
      .decode(BitVector.fromHex(message).get)
      .toOption
      .get
      .value

  // case class ADSB(DF:BitVector,CA:BitVector,ICAO:BitVector,DATA:BitVector,PI:BitVector)
  // val codec: Codec[ADSB] = ( bits(5) :: bits(3) :: bits(24) :: bits(56) :: bits(24)).as[ADSB]

  //case class ADSB(DF:BitVector,CA:BitVector,ICAO:BitVector,TC:BitVector, DATA:BitVector,PI:BitVector)

  //val r = codec.decode(ByteVector.fromHex(message).bits).toOption.get.value
  //r.map(_.value.toString)
}

class SDecoder extends Decoder

object Decoder {

  def decodeCharacter(bits:BitVector):Char = {
    bits.toByte(false) match {
      case v if 1 until 26+1 contains v => // A-Z 
        (65 + (v-1)).toChar
      case v if 48 until 57+1 contains v => // 0-9
        (48 + v-48).toChar
      case 32 => ' '
      case _ => '#'
    }
  }

  
  def decodeDataAsChars(bits:Seq[BitVector]):String = {
    bits.foldLeft("")(_ + decodeCharacter(_)).trim
  }

  val codecRawDF = (bits(5))
  
  //                                     DF         CA        ICAO        TC         DATA      PARITY/InterrogatorID
  val codecRawADSB: Codec[RawADSB] = (bits(5) :: bits(3) :: bits(24) :: bits(5) :: bits(51) :: bits(24)).as[RawADSB]
  
  val codecRawAircraftIdentification: Codec[RawAircraftIdentification] = (bits(3) :: bits(6) :: bits(6) :: bits(6) :: bits(6) :: bits(6) :: bits(6) :: bits(6) :: bits(6)).as[RawAircraftIdentification]
  val codecRawAirbornePositions: Codec[RawAirbornePosition] = (bits(2) :: bits(1) :: bits(12) :: bits(1) :: bits(1) :: bits(17) :: bits(17))
    .as[RawAirbornePosition]

  val decoder = new SDecoder
  def decode(data:String) = decoder.decode(data)

  def decodeDump1090(data:String) = decode(Dump1090.decode(data))
}
