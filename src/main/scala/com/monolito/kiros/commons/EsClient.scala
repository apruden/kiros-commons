package com.monolito.kiros.commons

import akka.actor.ActorSystem
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.json.{ JsonFormat, DefaultJsonProtocol }
import scala.concurrent.Future
import scala.util._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.stream.ActorMaterializer
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers
import akka.http.scaladsl.model.Uri.apply
import com.typesafe.config.ConfigFactory

object EsJsonProtocol extends DefaultJsonProtocol {
  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case n: Int => JsNumber(n)
      case l: Long => JsNumber(l)
      case s: String => JsString(s)
      case b: Boolean => if (b) JsTrue else JsFalse
      case q: Seq[Any] => JsArray(q.map(write(_)).toVector)
      case o: Map[String, Any] => JsObject(o.map(e => (e._1, write(e._2))))
      case x => JsString(x.toString)
    }

    def read(value: JsValue): Any = value match {
      case JsNumber(n) => if (n.intValue == n.longValue) n.intValue else n.longValue
      case JsString(s) => s
      case JsTrue => true
      case JsFalse => false
      case x: JsArray => x.elements.map(read(_))
      case JsNull => null
      case o: JsObject => o.fields.map(e => (e._1, read(e._2)))
    }
  }
}

object EsClient extends SprayJsonSupport with PredefinedToEntityMarshallers {
  import EsJsonProtocol._
  import concurrent.ExecutionContext.Implicits._
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val conf = ConfigFactory.load()
  val host = conf.getString("kiros.search-host")

  def createIndex(idx: String, mapping: Map[String, Any]): Future[Any] =
    for {
      req <- Marshal(mapping).to[RequestEntity]
      resp <- Http().singleRequest(HttpRequest(uri = s"$host/$idx", method = HttpMethods.POST, entity = req))
    } yield resp

  def esSave(idx: String, typ: String, document: Map[String, Any]): Future[Any] =
    for {
      req <- Marshal(document).to[RequestEntity]
      resp <- Http().singleRequest(HttpRequest(uri = s"$host/$idx/$typ", method = HttpMethods.POST, entity = req))
    } yield resp

  def put(idx: String, typ: String, id: String, document: Map[String, Any]): Future[Any] =
    for {
      req <- Marshal(document).to[RequestEntity]
      resp <- Http().singleRequest(HttpRequest(uri = s"$host/$idx/$typ/$id", method = HttpMethods.PUT, entity = req))
    } yield resp

  def get(idx: String, typ: String, id: String): Future[Option[Map[String, Any]]] =
    for {
      resp <- Http().singleRequest(HttpRequest(uri = s"$host/$idx/$typ/$id", method = HttpMethods.GET))
      d <- Unmarshal(resp.entity).to[Map[String, Any]]
      r <- Future {
        d.get("_source").asInstanceOf[Option[Map[String, Any]]]
          .flatMap(e => Some(e + ("_version" -> d.getOrElse("_version", 1))))
      }
    } yield r

  def del(idx: String, typ: String, id: String): Future[Unit] = ???

  def query(idx: String, typ: String, query: Map[String, Any]): Future[List[Map[String, Any]]] = {
    for {
      req <- Marshal(query + ("version" -> true)).to[RequestEntity]
      resp <- Http().singleRequest(HttpRequest(uri = s"$host/$idx/$typ/_search", method = HttpMethods.POST, entity = req))
      d <- Unmarshal(resp.entity).to[Map[String, Any]]
      r <- Future {
        d.getOrElse("hits", Map()).asInstanceOf[Map[String, Any]]
          .getOrElse("hits", List()).asInstanceOf[Seq[Map[String, Any]]]
          .map(h => h.get("_source").get.asInstanceOf[Map[String, Any]] + ("_version" -> h.getOrElse("_version", 1))).toList
      }
    } yield r
  }

  def aggs(idx: String, typ: String, query: Map[String, Any]): Future[List[Map[String, Any]]] = {
    for {
      req <- Marshal(query).to[RequestEntity]
      resp <- Http().singleRequest(HttpRequest(uri = s"$host/$idx/$typ/_search", method = HttpMethods.POST, entity = req))
      d <- Unmarshal(resp.entity).to[Map[String, Any]]
      r <- Future {
        d.getOrElse("aggregations", Map()).asInstanceOf[Map[String, Any]] //
          .getOrElse("result", Map()).asInstanceOf[Map[String, Any]] //
          .getOrElse("buckets", List()).asInstanceOf[Seq[Map[String, Any]]] //
          .toList
      }
    } yield r
  }
}
