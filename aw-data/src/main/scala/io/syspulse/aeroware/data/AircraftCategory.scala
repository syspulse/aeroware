package io.syspulse.aeroware.data

import scala.util.Random
import enumeratum._
import enumeratum.values._


object AircraftCategory extends IntEnum[AircraftCategory] { //with IntCirceEnum[AircraftCategory] with IntQuillEnum[AircraftCategory]{
  type CID = Int
  case object Unknown     extends AircraftCategory(value = 0, name = "Unknown")

  case object Airplane    extends AircraftCategory(value = 1, name = "Airplane")
  case object Rotorcraft  extends AircraftCategory(value = 2, name = "Rotorcraft")
  case object Glider      extends AircraftCategory(value = 3, name = "Glider")
  
  val values = findValues

  def withName(name:String): AircraftCategory = { 
    values.filter(_.name == name).headOption.getOrElse(Unknown)
   }

  def random() = withValue(Random.nextInt(AircraftCategoryRepo.db.size-1))
}

sealed abstract class AircraftCategory(val value: AircraftCategory.CID, val name: String) extends IntEnumEntry {
  override def toString = s"${this.getClass().getSimpleName}(${value},${name})"
}

object AircraftCategoryRepo {
  protected val default = AircraftCategory.values
  protected var custom: Seq[AircraftCategory] = Seq()
  def db = default ++ custom
  def +(c: AircraftCategory) = { custom = custom ++ Seq(c); this}

  def withName(name:String): Option[AircraftCategory] = { 
    (default.filter( _.name == name) ++ db.filter( _.name == name )).headOption
  }
}

case object Rocket extends AircraftCategory(value = 10, name = "Rocket")

// ----------------------------------------------------------------------------------------------------------
// how to use it with JSON
// import $ivy.`io.circe::circe-core:0.12.3`
// import $ivy.`io.circe::circe-generic:0.12.3`
// import $ivy.`io.circe::circe-parser:0.12.3`
// import $ivy.`com.beachape::enumeratum-circe:1.6.1`
// import io.circe.Json
// import io.circe.syntax._
// println(s"json: ${AircraftCategory.values.map(_.asJson)}")

// type AID = String
// case class Aircraft(id:AID,name:String,category:AircraftCategory)

// import io.circe._, io.circe.generic.auto._, io.circe.parser._
// val a1 = Aircraft("A0001","Cessna-172",Airplane)
// println(s"a1 = ${a1}")
// println(s"a1 = ${a1.asJson}")

// ----------------------------------------------------------------------------------------------------------
// how to use it with DB
// Quill import MUST go before enumeratum-quill
// import $ivy.`io.getquill::quill-jdbc:3.5.2`
// import $ivy.`mysql:mysql-connector-java:8.0.22`
// import $ivy.`com.beachape::enumeratum-quill:1.6.0`
// import $ivy.`org.postgresql:postgresql:42.2.8`
// import $ivy.`com.opentable.components:otj-pg-embedded:0.13.1`

// import com.opentable.db.postgres.embedded.EmbeddedPostgres
// val server = EmbeddedPostgres.builder().setPort(15432).start()

// val config = """ctx.dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
// ctx.dataSource.user=postgres
// ctx.dataSource.password=
// ctx.dataSource.databaseName=postgres
// ctx.dataSource.portNumber=15432
// ctx.dataSource.serverName=localhost
// ctx.connectionTimeout=30000"""
// .split("\\n").foreach(s=>{println(s);val p=s.split("="); System.setProperty(p(0),if(p.size<2) "" else p(1))})

// import io.getquill._
// lazy val ctx = new PostgresJdbcContext(SnakeCase, "ctx")
// //lazy val ctx = new MysqlJdbcContext(SnakeCase, "ctx")
// import ctx._


// ctx.executeAction(s"CREATE TABLE IF NOT EXISTS gamet (id VARCHAR(36) PRIMARY KEY);")
// case class Gamet(id:String)
// val g1  = Gamet(uuid())
// println(s"g1 = ${g1}")
// val q = quote { 
//   query[Gamet].insert(lift(g1)) 
// }
// ctx.run(q)
// ctx.run(query[Gamet]).foreach{ r => println(s"g=${r}") }


// ctx.executeAction(s"CREATE TABLE IF NOT EXISTS aircraft (id VARCHAR(36) PRIMARY KEY, name VARCHAR(128), category INTEGER);")

// ctx.run(quote {query[Aircraft].insert(
//   lift(Aircraft(uuid(),"Cessna-172",AircraftCategory.random())
// ))})
// ctx.run(query[Aircraft]).foreach{ r => println(s"a=${r}") }