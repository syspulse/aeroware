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
    r.result.to[Aircraft]
  }

  override def all(from:Long,size:Long):Seq[Aircraft] = {    
    val r = client.execute {
      ElasticDsl
      .search(uri.index)
      .from(from.toInt)
      .limit(size.toInt)
      .matchAllQuery()
    }.await

    log.info(s"r=${r}")
    r.result.to[Aircraft]
  }
    

  // slow and memory hungry !
  def size:Long = {
    val r = client.execute {
      ElasticDsl.count(Indexes(uri.index))
    }.await
    r.result.count
  }

  def +(a:Aircraft):Try[Aircraft] = { 
    val r = { client.execute { 
      indexInto(uri.index).fields(
        "id" -> a.id,
        "rid" -> a.rid,
        "model" -> a.model,
        "typ" -> a.typ,
        "call" -> a.call.getOrElse("")
      )
      .refresh(RefreshPolicy.IMMEDIATE)        
    }}
    .await
    
    if(r.isSuccess)
      Success(a)
    else
      Failure(r.error.asException)
  }

  def del(id:ID):Try[ID] = { 
    val r = { client.execute { 
      deleteByQuery(uri.index,
        termQuery("id",id)        
      )      
    }}
    .await
    
    if(r.isSuccess)
      Success(id)
    else
      Failure(r.error.asException)
  }

  // def ?(id:ID):Try[Aircraft] = {
  //   search(id.toString).take(1).headOption match {
  //     case Some(y) => Success(y)
  //     case None => Failure(new Exception(s"not found: ${id}"))
  //   }
  // }
  def ?(id:ID):Try[Aircraft] = {
    val r = { client.execute { 
      ElasticDsl
        .search(uri.index)
        .termQuery(("id",id))
    }}.await

    val rr = r.result.to[Aircraft]
    if(rr.size > 0) 
      Success(rr(0))
    else
      Failure(new Exception(s"not found: ${id}"))        
  }

  override def ??(id:Seq[ID]):Seq[Aircraft] = {
    val r = { client.execute { 
      ElasticDsl
        .search(uri.index)
        .query(termsQuery("id",id))        
    }}.await

    val rr = r.result.to[Aircraft]
    rr
  }
  
  def scan(txt:String):Seq[Aircraft] = {
    val r = client.execute {
      ElasticDsl
        .search(uri.index)
        .rawQuery(s"""
    { 
      "query_string": {
        "query": "${txt}",
        "fields": ["id", "rid","call","model"]
      }
    }
    """)        
    }.await

    log.info(s"r=${r}")
    r.result.to[Aircraft]
  }

  def search(txt:String,from:Long,size:Long):Seq[Aircraft] = {   
    val r = client.execute {
      ElasticDsl
        .search(uri.index)
        .from(from.toInt)
        .size(size.toInt)
        .query(txt)
    }.await

    log.info(s"r=${r}")
    r.result.to[Aircraft]
  }

  def grep(txt:String,from:Long,size:Long):Seq[Aircraft] = {
    val r = client.execute {
      ElasticDsl
        .search(uri.index)
        .from(from.toInt)
        .size(size.toInt)
        .query {
          ElasticDsl.wildcardQuery("id",txt)
        }        
    }.await

    log.info(s"r=${r}")
    r.result.to[Aircraft]
  }

  def typing(txt:String,size:Int):Seq[Aircraft] = {
    val r = client.execute {
      ElasticDsl
        .search(uri.index)
        .rawQuery(s"""
    { "multi_match": { "query": "${txt}", "type": "bool_prefix", "fields": [ "id._3gram","rid._3gram","call._3gram","model._3gram" ] }}
    """)
        .size(size)
    }.await
    
    log.info(s"r=${r}")
    r.result.to[Aircraft]
  }
}
