package com.monolito.kiros.commons

import scala.concurrent.Future
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.directives.SecurityDirectives.AsyncAuthenticator

trait OAuth2Support {
  val authenticator: String => AsyncAuthenticator[OAuthCred] = (scope) =>
    (credentials) => credentials match {
      case Provided(identifier) => validateToken(Some(identifier), scope)
      case _ => Future.successful(None)
    }

  def validateToken(tok: Option[String], scope: String): Future[Option[OAuthCred]] = {
    import java.util.Base64
    val cred = if (tok.nonEmpty) {
      new String(Base64.getDecoder.decode(tok.get), "UTF-8").split('|').toList match {
        case List(data, hmac) =>
          data.split(':').toList match {
            case List(uid, scopes, expire) => {
              if (true)
                Some(OAuthCred(uid, scopes.split(' ').toList, expire.toLong))
              else
                None
            }
            case _ => None
          }
        case _ => None
      }
    } else None

    Future.successful {
      cred match {
        case cred: Some[OAuthCred] => if (cred.get.anyScope(List(scope))) cred else None
        case _ => None
      }
    }
  }
}

case class OAuthCred(id: String, scopes: List[String], expire: Long) {
  def anyScope(requiredScopes: List[String]): Boolean = !scopes.intersect(requiredScopes).isEmpty
}
