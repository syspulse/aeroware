package io.syspulse.aeroware.aircraft.store

import scala.util.Try
import scala.util.{Success,Failure}
import scala.collection.immutable

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.Logger

import io.jvm.uuid._

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl
import com.sksamuel.elastic4s.fields.TextField
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.SearchResponse

import io.syspulse.aeroware.aircraft._
import io.syspulse.aeroware.aircraft.Aircraft.ID

import io.syspulse.skel.uri.ElasticURI

class AircraftStoreElastic(elasticUri:String) extends AircraftStore {  
  private val log = Logger(s"${this}")

  val uri = ElasticURI(elasticUri)

  implicit object AircraftHitReader extends HitReader[Aircraft] {
    // becasue of VID case class, it is converted unmarchsalled as Map from Elastic (field vid.id)
    override def read(hit: Hit): Try[Aircraft] = {
      val source = hit.sourceAsMap
      Success(
        Aircraft(
          id = source("id").asInstanceOf[String],
          rid = source("rid").asInstanceOf[String],
          model = source("model").asInstanceOf[String],
          typ = source("typ").asInstanceOf[String],
          call = source("call").asInstanceOf[String] match {
            case "" => None
            case c => Some(c)
          },
          ts = source("ts").asInstanceOf[Long], 
        )
      )
    }
  }
  
  val client = ElasticClient(JavaClient(ElasticProperties(uri.uri)))

  import ElasticDsl._  
  def all:Seq[Aircraft] = {    
    val r = client.execute {
      ElasticDsl
      .search(uri.index)
      .matchAllQuery()
    }.await

    log.info(s"r=${r}")
    r.result.to[Aircraft].toList
  }

  // slow and memory hungry !
  def size:Long = {
    val r = client.execute {
      ElasticDsl.count(Indexes(uri.index))
    }.await
    r.result.count
  }

  def +(Aircraft:Aircraft):Try[Aircraft] = { 
    Failure(new UnsupportedOperationException(s"not implemented: ${Aircraft}"))
  }

  def del(id:ID):Try[ID] = { 
    Failure(new UnsupportedOperationException(s"not implemented: ${id}"))
  }

  def ?(id:ID):Try[Aircraft] = {
    search(id.toString).take(1).headOption match {
      case Some(y) => Success(y)
      case None => Failure(new Exception(s"not found: ${id}"))
    }
  }

  def ??(txt:String):List[Aircraft] = {
    search(txt)
  }

  def scan(txt:String):List[Aircraft] = {
    val r = client.execute {
      ElasticDsl
        .search(uri.index)
        .rawQuery(s"""
    { 
      "query_string": {
        "query": "${txt}",
        "fields": ["area", "msg"]
      }
    }
    """)        
    }.await

    log.info(s"r=${r}")
    r.result.to[Aircraft].toList
  }

  def search(txt:String):List[Aircraft] = {   
    val r = client.execute {
      com.sksamuel.elastic4s.ElasticDsl
        .search(uri.index)
        .query(txt)
    }.await

    log.info(s"r=${r}")
    r.result.to[Aircraft].toList
  }

  def grep(txt:String):List[Aircraft] = {
    val r = client.execute {
      ElasticDsl
        .search(uri.index)
        .query {
          ElasticDsl.wildcardQuery("msg",txt)
        }
    }.await

    log.info(s"r=${r}")
    r.result.to[Aircraft].toList
  }

  def typing(txt:String):List[Aircraft] = {  
    val r = client.execute {
      ElasticDsl
        .search(uri.index)
        .rawQuery(s"""
    { "multi_match": { "query": "${txt}", "type": "bool_prefix", "fields": [ "msg._3gram" ] }}
    """)        
    }.await
    
    log.info(s"r=${r}")
    r.result.to[Aircraft].toList
  }
}
