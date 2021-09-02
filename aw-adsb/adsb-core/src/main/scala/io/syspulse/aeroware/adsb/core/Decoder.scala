package io.syspulse.aeroware.adsb.core

import scala.util.{Try,Success,Failure}
import scala.math._

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
import io.syspulse.aeroware.core._
import io.syspulse.aeroware.core.Units

case class RawALT(a1:BitVector,Q:BitVector,a2:BitVector) {
  override def toString = {
    s"RawALT(a1=${a1.toByte()},Q=${Q.toBin},a1=${a1.toByte()})"
  }
}

case class RawAirbornePosition(SS: BitVector,NICsb: BitVector,ALT: BitVector,T: BitVector,F: BitVector,
  LAT_CPR: BitVector,LON_CPR: BitVector) {
  override def toString = {
    s"RawAirbornePosition(SS=${SS.toByte()},NICsb=${NICsb.toBin},ALT=${ALT
      .toBin},T=${T.toBin} F=${F.toBin},LAT_CPR=${LAT_CPR
      .toLong()},LON_CPR=${LON_CPR.toLong()})"
  }

  val isOdd = F.toByte(false) == 1
  val latCPR = LAT_CPR.toLong(false).toDouble
  val lonCPR = LON_CPR.toLong(false).toDouble


  def getAltitude:Altitude = {
    val codecRawALT: Codec[RawALT] = (bits(7) :: bits(1) :: bits(4)).as[RawALT]
    val rawAltOpt = codecRawALT.decode(ALT).toOption
    
    if(!rawAltOpt.isDefined) return Altitude(0,Units.METERS)
    var rawAlt = rawAltOpt.get.value

    val a = rawAlt.Q.toByte(false) match {
      case 0 => 50175.0 // not implemented
      case 1 => (rawAlt.a1 ++ rawAlt.a2).toLong(false).toDouble * 25.0 - 1000.0
    }

    Altitude(a ,Units.FEET)
  }

  def getLocalPosition(ref:Location):Location = {
    Decoder.getLocalPosition(ref,isOdd,latCPR,lonCPR, getAltitude)
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

abstract class ADSB_Decoder(decoderLocation:Location) {
  val log = Logger(this.getClass().getSimpleName())

  
  def decodeAircraftAddr(b:BitVector):AircraftAddress = {
    val icaoId = b.toHex.toLowerCase
    val aircraft = AircraftICAORegistry.find(icaoId)
    val (icaoType,icaoCallsign) = if(aircraft.isDefined) (aircraft.get.icaoType,aircraft.get.regid) else ("","")
        AircraftAddress(icaoId,icaoType,icaoCallsign)    
  }


  def decode(data: String, refLoc:Location = decoderLocation): Try[ADSB] = {
    val message = data.trim
    if(message.size == 0 || message.size < 14 || message.size > 28 ) 
      return Failure(new Exception(s"invalid size: ${message.size}"))

    val df = try {
      Decoder.codecRawDF.decode(BitVector.fromHex(message).get).toOption.get.value.toByte(false)
    } catch {
      case e:Exception => return Failure(new Exception(s"invalid format: failed to parse DF: ${data}"))
    }
    
    val ts = System.currentTimeMillis

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
              raw = message, ts)
          }
          case v if 5 until 9 contains v => ADSB_SurfacePosition(df,capability,aircraftAddr,raw = message)
          case v if 9 until 19 contains v => {
            val a = Decoder.codecRawAirbornePositions.decode(raw.DATA).toOption.get.value
            val loc = a.getLocalPosition(refLoc)
            ADSB_AirbornePositionBaro(df,capability,aircraftAddr,
              loc, a.isOdd, a.latCPR, a.lonCPR, 
              raw = message, ts)
          }
          case 19                          => ADSB_AirborneVelocity(df,capability,aircraftAddr,raw = message, ts)
          case v if 20 until 23 contains v => ADSB_AirbornePositionGNSS(df,capability,aircraftAddr,raw = message, ts)
          case v if 23 until 28 contains v => ADSB_Reserved(df,capability,aircraftAddr,raw = message, ts)
          case 28                          => ADSB_AircraftStatus(df,capability,aircraftAddr,raw = message, ts)
          case 29                          => ADSB_TargetState(df,capability,aircraftAddr,raw = message,ts)
          case 31                          => ADSB_AircraftOperationStatus(df,capability,aircraftAddr,raw = message,ts)
          case _                           => ADSB_Unknown(df,capability,aircraftAddr,raw = message,ts)
        }
      // case 18 => // non-interrogatable equipment
      //   //log.warn(s"msg=${message}: DF=${df}: Unsupported DF")
      //   ADSB_Unknown(df,0,AircraftAddress("0","",""),raw = message,ts)
      case _ => // unknown
        //log.warn(s"msg=${message}: DF=${df}: Unsupported DF")
        ADSB_Unknown(df,0,AircraftAddress("0","",""),raw = message,ts)
    }
    Success(adsb)
  }

  def decodeAirbornePosition(message: String): RawAirbornePosition =
    Decoder.codecRawAirbornePositions
      .decode(BitVector.fromHex(message).get)
      .toOption
      .get
      .value

}

class Decoder(val decoderLocation:Location = Location(50.4584,30.3381,Altitude(221,Units.METERS))) extends ADSB_Decoder(decoderLocation)

object Decoder {
  val log = Logger(this.getClass().getSimpleName())

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

  private def mod(a:Double, b:Double) = ((a%b)+b)%b

  private def NL(rLat:Double):Double = {
		if (rLat == 0) return 59.0
		  else 
    if (abs(rLat) == 87) return 2.0
		  else 
    if (abs(rLat) > 87) return 1.0

		floor( 2.0 * Math.PI / acos(1 - (1 - cos(Math.PI/(2.0*15.0))) / pow(cos(Math.PI/180.0 * abs(rLat)), 2)))
	}

  def getLocalPosition(ref:Location, isOdd:Boolean, latCPR:Double, lonCPR:Double, alt:Altitude):Location = {
		
    val dLat = 360.0 / (if(isOdd) 59.0 else 60.0)
		val j = floor(ref.lat / dLat) + floor(mod(ref.lat,dLat) / dLat - latCPR / 131072.0 + 0.5)
		val lat = dLat * (j + latCPR / 131072.0)
		val dLon = 360.0 / max(1.0, NL(lat) - (if(isOdd) 1.0 else 0.0))
		val m = floor(ref.lon / dLon) + floor(0.5 + mod(ref.lon, dLon) / dLon - lonCPR / 131072.0)
		val lon = dLon * (m + lonCPR / 131072.0 )

    //println(s"latCPR=${latCPR}, lonCPR=${lonCPR}, dLat=${dLat}, j=${j}, rLat=${lat}, dLon=${dLon}, m=${m}, lon=${lon}")

		Location(lat, lon, alt);
	}


  // a0 - previous event
  // a1 - current event
  def getGloballPosition(a0:ADSB_AirbornePositionBaro,a1:ADSB_AirbornePositionBaro): Location = {
    if(a0.aircraftAddr != a1.aircraftAddr) {
      log.warn(s"different aircrafts: ${a0.aircraftAddr} : ${a1.aircraftAddr}")
      return a1.loc
    }
		
		if (a0.isOdd == a1.isOdd) {
      log.warn(s"same odds (expected different): ${a0.isOdd} : ${a1.isOdd}")
      return a1.loc
    }
		
		val (even,odd) = if(a1.isOdd) (a0,a1) else (a1,a0)
		
		val dLat0 = 360.0 / 60.0;
		val dLat1 = 360.0 / 59.0;

		val j = floor((59.0 * even.latCPR - 60.0 * odd.latCPR) / 131072.0 + 0.5);

		var rLat0 = dLat0 * (mod(j, 60.0) + even.latCPR / 131072.0);
		var rLat1 = dLat1 * (mod(j, 59.0) + odd.latCPR / 131072.0);

		// Southern hemisphere
		if (rLat0 >= 270.0 && rLat0 <= 360.0)	rLat0 = rLat0 - 360.0;
		if (rLat1 >= 270.0 && rLat1 <= 360.0) rLat1 = rLat1 - 360.0;

		if (NL(rLat0) != NL(rLat1)) {
			log.warn(s"incompatible number of even longitudal zones: ${rLat0} : ${rLat1}: must be equal")
      return a1.loc
    }

		val NL_0 = NL(rLat0) 
		val NL_1 = max(1.0, NL_0 - (if(a1.isOdd) 1.0 else 0.0))
		val dLon = 360.0 / NL_1;

		val m = floor(( even.lonCPR * (NL_0 - 1.0) - odd.lonCPR * NL_0) / 131072.0 + 0.5);

		var rLon = dLon * (mod(m, NL_1)	+ ( if(a1.isOdd) odd.lonCPR else even.lonCPR ) / 131072.0)

		// ecuatorial longitude
		if (rLon < -180.0 && rLon > -360.0) rLon = rLon + 360.0;
		if (rLon > 180.0 && rLon < 360.0)	rLon = rLon - 360.0;

		val alt = a1.loc.alt;
		Location(if(a1.isOdd) rLat1 else rLat0, rLon, alt)
  }

  
  def decodeDataAsChars(bits:Seq[BitVector]):String = {
    bits.foldLeft("")(_ + decodeCharacter(_)).trim
  }

  val codecRawDF = (bits(5))
  
  //                                     DF         CA        ICAO        TC         DATA      PARITY/InterrogatorID
  val codecRawADSB: Codec[RawADSB] = (bits(5) :: bits(3) :: bits(24) :: bits(5) :: bits(51) :: bits(24))
    .as[RawADSB]
  
  val codecRawAircraftIdentification: Codec[RawAircraftIdentification] = (bits(3) :: bits(6) :: bits(6) :: bits(6) :: bits(6) :: bits(6) :: bits(6) :: bits(6) :: bits(6))
    .as[RawAircraftIdentification]
  
  //                                                            SS         NICsb       ALT         T          F        LAT-CPR     LON-SPR 
  val codecRawAirbornePositions: Codec[RawAirbornePosition] = (bits(2) :: bits(1) :: bits(12) :: bits(1) :: bits(1) :: bits(17) :: bits(17))
    .as[RawAirbornePosition]

  val decoder = new Decoder
  def decode(data:String) = decoder.decode(data)

  def decodeDump1090(data:String) = decode(Dump1090.decode(data))
}
