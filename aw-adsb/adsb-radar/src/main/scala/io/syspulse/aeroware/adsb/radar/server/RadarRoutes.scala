package io.syspulse.aeroware.adsb.radar.server

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.model.StatusCodes._
import com.typesafe.scalalogging.Logger

import io.jvm.uuid._

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.parameters.RequestBody

import jakarta.ws.rs.{Consumes, POST, PUT, GET, DELETE, Path, Produces}
import jakarta.ws.rs.core.MediaType

import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings


import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter

import io.syspulse.skel.service.Routeable
import io.syspulse.skel.service.CommonRoutes

import io.syspulse.skel.Command

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import io.syspulse.skel.auth.permissions.rbac.Permissions
import io.syspulse.skel.auth.RouteAuthorizers

import io.syspulse.skel.util.TimeUtil
import scala.util.Try

import io.syspulse.aeroware.adsb.radar.store.RadarRegistry._

@Path(s"/")
class RadarRoutes(registry: ActorRef[Command])(implicit context: ActorContext[_]) extends CommonRoutes with Routeable with RouteAuthorizers {
  //val log = Logger(s"${this}")
  implicit val system: ActorSystem[_] = context.system
  
  implicit val permissions = Permissions()

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import RadarProto._
  
  // registry is needed because Unit-tests with multiple Routes in Suites will fail (Prometheus libary quirk)
  val cr = new CollectorRegistry(true);
  val metricGetCount: Counter = Counter.build().name("radar_get_total").help("Radar gets").register(cr)
  
  def getRadarTelemetry(): Future[Try[RadarTelemetry]] = registry.ask(GetRadarTelemetry)
  def getRadarTelemetry(aid: String,ts0:Long, ts1:Long): Future[Try[RadarTelemetry]] = registry.ask(GetRadarTelemetryTime(aid, ts0,ts1, _))
  // def getRadarLast(id: ID): Future[Try[Radar]] = registry.ask(GetRadarLast(id, _))  

  @GET @Path("/{aid}") @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("radar"),summary = "Return Radar Telemetr by aid in time range",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, description = "Aircraft id"),
      new Parameter(name = "ts0", in = ParameterIn.PATH, description = "Start Timestamp (millisec) (optional)"),
      new Parameter(name = "ts1", in = ParameterIn.PATH, description = "End Timestamp (millisec) (optional)"),      
    ),
    responses = Array(new ApiResponse(responseCode="200",description = "Telemtry returned",content=Array(new Content(schema=new Schema(implementation = classOf[RadarTelemetry])))))
  )
  def getRadarTelemtryRoute(aid: String) = get {
    rejectEmptyResponse {
      parameters("ts0".as[String].optional, "ts1".as[String].optional) { (ts0, ts1) => 
        onSuccess(getRadarTelemetry(aid,
          TimeUtil.wordToTs(ts0.getOrElse(""),0L).get,
          TimeUtil.wordToTs(ts1.getOrElse(""),Long.MaxValue-1).get)) { r =>
          
          metricGetCount.inc()
          encodeResponse(complete(r))
        }
      }
    }
  }

  // @GET @Path("/{id}/last") @Produces(Array(MediaType.APPLICATION_JSON))
  // @Operation(tags = Array("Radar"),summary = "Return last Radar by id",
  //   parameters = Array(
  //     new Parameter(name = "id", in = ParameterIn.PATH, description = "Radar id"),
  //   ),
  //   responses = Array(new ApiResponse(responseCode="200",description = "Last Radar returned",content=Array(new Content(schema=new Schema(implementation = classOf[Radar])))))
  // )
  // def getRadarLastRoute(id: String) = get {
  //   rejectEmptyResponse {
  //     onSuccess(getRadarLast(id)) { r =>
  //       metricGetCount.inc()
  //       encodeResponse(complete(r))
  //     }
  //   }
  // }


  @GET @Path("/") @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("Radar"), summary = "Return all Radar Telemetry",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Telemtry",content = Array(new Content(schema = new Schema(implementation = classOf[RadarTelemetry])))))
  )
  def getRadarTelemtryRoute() = get {
    metricGetCount.inc()
    encodeResponse(complete(getRadarTelemetry()))
  }

  
  val corsAllow = CorsSettings(system.classicSystem)
    //.withAllowGenericHttpRequests(true)
    .withAllowCredentials(true)
    .withAllowedMethods(Seq(HttpMethods.OPTIONS,HttpMethods.GET,HttpMethods.POST,HttpMethods.PUT,HttpMethods.DELETE,HttpMethods.HEAD))

  override def routes: Route = cors(corsAllow) {
      concat(
        pathEndOrSingleSlash {
          concat(
            authenticate()(authn =>              
              getRadarTelemtryRoute()
            ),            
          )
        },
        // pathPrefix("search") {
        //   pathPrefix(Segment) { txt => 
        //     authenticate()(authn => {
        //       getRadarSearch(txt)
        //     })
        //   }
        // },
        pathPrefix(Segment) { aid =>         
          // pathSuffix("last") {
          //   getRadarLastRoute(id)
          // } ~
          pathEndOrSingleSlash {
            authenticate()(authn =>
              getRadarTelemtryRoute(aid)              
            ) 
          }
        }
      )
  }
    
}
