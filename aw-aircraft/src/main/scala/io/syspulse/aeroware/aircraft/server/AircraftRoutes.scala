package io.syspulse.aeroware.aircraft.server

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
// import javax.ws.rs.{Consumes, POST, GET, DELETE, Path, Produces}
// import javax.ws.rs.core.MediaType
import jakarta.ws.rs.{Consumes, POST, GET, DELETE, Path, Produces}
import jakarta.ws.rs.core.MediaType

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter

import io.syspulse.skel.service.Routeable
import io.syspulse.skel.service.CommonRoutes

import io.syspulse.skel.Command

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import io.syspulse.skel.auth.permissions.Permissions
import io.syspulse.skel.auth.RouteAuthorizers

import io.syspulse.aeroware.aircraft._
import io.syspulse.aeroware.aircraft.Aircraft.ID
import io.syspulse.aeroware.aircraft.store.AircraftRegistry
import io.syspulse.aeroware.aircraft.store.AircraftRegistry._
import scala.util.Try


@Path("/")
class AircraftRoutes(registry: ActorRef[Command])(implicit context: ActorContext[_]) extends CommonRoutes with Routeable with RouteAuthorizers {
  //val log = Logger(s"${this}")
  implicit val system: ActorSystem[_] = context.system
  
  implicit val permissions = Permissions()

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import AircraftJson._
  
  // registry is needed because Unit-tests with multiple Routes in Suites will fail (Prometheus libary quirk)
  val cr = new CollectorRegistry(true);
  val metricGetCount: Counter = Counter.build().name("aw_aircraft_get_total").help("Aircraft gets").register(cr)
  val metricDeleteCount: Counter = Counter.build().name("aw_aircraft_delete_total").help("Aircraft deletes").register(cr)
  val metricCreateCount: Counter = Counter.build().name("aw_aircraft_create_total").help("Aircraft creates").register(cr)
  
  def getAircrafts(): Future[Aircrafts] = registry.ask(GetAircrafts)
  def getAircraft(id: ID): Future[Try[Aircraft]] = registry.ask(GetAircraft(id, _))
  def searchAircraft(txt: String): Future[Aircrafts] = registry.ask(SearchAircraft(txt, _))

  def createAircraft(AircraftCreate: AircraftCreateReq): Future[Aircraft] = registry.ask(CreateAircraft(AircraftCreate, _))
  def deleteAircraft(id: ID): Future[AircraftRes] = registry.ask(DeleteAircraft(id, _))
  //def randomAircraft(): Future[Aircraft] = registry.ask(RandomAircraft(_))


  @GET @Path("/{id}") @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("Aircraft"),summary = "Return Aircraft by id",
    parameters = Array(new Parameter(name = "id", in = ParameterIn.PATH, description = "Aircraft id (uuid)")),
    responses = Array(new ApiResponse(responseCode="200",description = "Aircraft returned",content=Array(new Content(schema=new Schema(implementation = classOf[Aircraft])))))
  )
  def getAircraftRoute(id: String) = get {
    rejectEmptyResponse {
      onSuccess(getAircraft(id)) { r =>
        metricGetCount.inc()
        complete(r)
      }
    }
  }


  @GET @Path("/search/{txt}") @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("Aircraft"),summary = "Search Aircraft",
    parameters = Array(new Parameter(name = "txt", in = ParameterIn.PATH, description = "text to search")),
    responses = Array(new ApiResponse(responseCode="200",description = "Aircraft returned",content=Array(new Content(schema=new Schema(implementation = classOf[Aircraft])))))
  )
  def searchAircraftRoute(txt: String) = get {
    rejectEmptyResponse {
      onSuccess(searchAircraft(txt)) { r =>
        complete(r)
      }
    }
  }

  @GET @Path("/") @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("Aircraft"), summary = "Return all Aircrafts",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of Aircrafts",content = Array(new Content(schema = new Schema(implementation = classOf[Aircrafts])))))
  )
  def getAircraftsRoute() = get {
    metricGetCount.inc()
    complete(getAircrafts())
  }

  @DELETE @Path("/{id}") @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("Aircraft"),summary = "Delete Aircraft by id",
    parameters = Array(new Parameter(name = "id", in = ParameterIn.PATH, description = "Aircraft id (uuid)")),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Aircraft deleted",content = Array(new Content(schema = new Schema(implementation = classOf[Aircraft])))))
  )
  def deleteAircraftRoute(id: String) = delete {
    onSuccess(deleteAircraft(id)) { r =>
      metricDeleteCount.inc()
      complete((StatusCodes.OK, r))
    }
  }

  @POST @Path("/") @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(tags = Array("Aircraft"),summary = "Create Aircraft Secret",
    requestBody = new RequestBody(content = Array(new Content(schema = new Schema(implementation = classOf[AircraftCreateReq])))),
    responses = Array(new ApiResponse(responseCode = "200", description = "Aircraft created",content = Array(new Content(schema = new Schema(implementation = classOf[AircraftRes])))))
  )
  def createAircraftRoute = post {
    entity(as[AircraftCreateReq]) { AircraftCreate =>
      onSuccess(createAircraft(AircraftCreate)) { r =>
        metricCreateCount.inc()
        complete((StatusCodes.Created, r))
      }
    }
  }

  override def routes: Route =
      concat(
        pathEndOrSingleSlash {
          // authenticate()(authn =>
          //   authorize(Permissions.isAdmin(authn)) {
          //     concat(
          //       getAircraftsRoute(),
          //       createAircraftRoute
          //     )
          //   }
          // )
          concat(
            authenticate()(authn =>
              authorize(Permissions.isAdmin(authn)) {              
                getAircraftsRoute() ~                
                createAircraftRoute  
              }
            ),
            //createAircraftRoute
          )
        },
        // pathPrefix("info") {
        //   path(Segment) { AircraftId => 
        //     getAircraftInfo(AircraftId)
        //   }
        // },        
        pathPrefix("search") {
          pathPrefix(Segment) { txt => 
            searchAircraftRoute(txt)
          }
        },
        pathPrefix(Segment) { id => 
          // pathPrefix("eid") {
          //   pathEndOrSingleSlash {
          //     searchAircraftRoute(id)
          //   } 
          //   ~
          //   path(Segment) { code =>
          //     getAircraftCodeVerifyRoute(id,code)
          //   }
          // } ~

          pathEndOrSingleSlash {
            // concat(
            //   getAircraftRoute(id),
            //   deleteAircraftRoute(id),
            // )          
            authenticate()(authn =>
              authorize(Permissions.isAdmin(authn)) {
                getAircraftRoute(id)
              } ~
              authorize(Permissions.isAdmin(authn)) {
                deleteAircraftRoute(id)
              }
            ) 
          }
        }
      )
    
}
