/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.crdlcacheadminfrontend.connectors

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.crdlcacheadminfrontend.config.AppConfig
import uk.gov.hmrc.crdlcacheadminfrontend.codeLists.models.*
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.models.*
import uk.gov.hmrc.crdlcacheadminfrontend.utils.Logging
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.UpstreamErrorResponse.{Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant

@Singleton
class CRDLConnector @Inject() (config: AppConfig, httpClient: HttpClientV2)(using
  system: ActorSystem
) extends Retries
  with Logging {
  override protected def actorSystem: ActorSystem = system
  override protected def configuration: Config    = config.config.underlying

  private val crdlCacheCodeListsUrl      = s"${config.crdlCacheUrl}/lists"
  private val crdlCacheCustomsOfficesUrl = s"${config.crdlCacheUrl}/offices"

  def fetchCodeListSnapShots()(using
    hc: HeaderCarrier,
    ex: ExecutionContext
  ): Future[List[CodeListSnapshot]] = {
    // Use the internal-auth token to call the crdl-cache service
    val hcWithInternalAuth =
      hc.copy(authorization = Some(Authorization((config.internalAuthToken))))
    logger.info(s"Fetching codelist snapshots from crdl-cache")
    val fetchResult = retryFor(s"fetch of codelist snapshots") {
      // No point in retrying if our request is wrong
      case Upstream4xxResponse(_) => false
      // Attempt to recover from intermittent connectivity issues
      case Upstream5xxResponse(_) => true
    } {
      httpClient
        .get(url"${config.crdlCacheUrl}/lists")(using hcWithInternalAuth)
        .execute[List[CodeListSnapshot]](using throwOnFailure(readEitherOf[List[CodeListSnapshot]]))
    }
    fetchResult.failed.foreach(err =>
      logger.error("Retries exceeded while fetching code list snapshots", err)
    )
    fetchResult
  }

  private def urlForCodeList(
    code: String,
    filterKeys: Option[Set[String]],
    filterProperties: Option[Map[String, Any]]
  ): URL =
    (filterKeys, filterProperties) match {
      case (Some(keys), Some(properties)) =>
        url"$crdlCacheCodeListsUrl/${code}?keys=${keys.mkString(",")}&$properties"
      case (Some(keys), None) =>
        url"$crdlCacheCodeListsUrl/${code}?keys=${keys.mkString(",")}"
      case (None, Some(properties)) =>
        url"$crdlCacheCodeListsUrl/${code}?$properties"
      case (None, None) =>
        url"$crdlCacheCodeListsUrl/${code}"
    }

  def fetchCodeList(
    code: String,
    filterKeys: Option[Set[String]] = None,
    filterProperties: Option[Map[String, Any]] = None
  )(using hc: HeaderCarrier, ec: ExecutionContext): Future[List[CodeListEntry]] = {
    val hcWithInternalAuth = hc.copy(authorization = Some(Authorization(config.internalAuthToken)))
    logger.info(s"Fetching ${code} codelist from crdl-cache")
    val fetchResult = retryFor(s"fetch of codelist entries for ${code}") {
      case Upstream4xxResponse(_) => false
      case Upstream5xxResponse(_) => true
    } {
      httpClient
        .get(urlForCodeList(code, filterKeys, filterProperties))(using hcWithInternalAuth)
        .execute[List[CodeListEntry]](using throwOnFailure(readEitherOf[List[CodeListEntry]]))
    }
    fetchResult.failed.foreach(err =>
      logger.error(s"Retries exceeded while fetching ${code} ", err)
    )
    fetchResult
  }

  private def urlForCustomsOffices(
    referenceNumbers: Option[Set[String]],
    countryCodes: Option[Set[String]],
    roles: Option[Set[String]],
    activeAt: Option[Instant]
  ): URL = {
    val url = (referenceNumbers, countryCodes, roles, activeAt) match {
      case (None, None, None, None) => crdlCacheCustomsOfficesUrl
      case _ =>
        crdlCacheCustomsOfficesUrl
          .concat(
            Seq(
              referenceNumbers.fold("")(r => s"referenceNumbers=${r.mkString(",")}"),
              countryCodes.fold("")(c => s"countryCodes=${c.mkString(",")}"),
              roles.fold("")(r => s"roles=${r.mkString(",")}")
            ).filterNot(_.isEmpty).mkString("?", "&", "")
          )
    }
    url"$url"
  }

  def fetchCustomsOffices(
    referenceNumbers: Option[Set[String]] = None,
    countryCodes: Option[Set[String]] = None,
    roles: Option[Set[String]] = None,
    activeAt: Option[Instant] = None
  )(using hc: HeaderCarrier, ec: ExecutionContext): Future[List[CustomsOffice]] = {
    val hcWithInternalAuth = hc.copy(authorization = Some(Authorization(config.internalAuthToken)))
    logger.info(s"Fetching customs offices from crdl-cache")
    val fetchResult = retryFor(
      referenceNumbers.fold(s"fetching all customs offices")(r =>
        s"fetching customs offices for ${r.mkString(",")}"
      )
    ) {
      case Upstream4xxResponse(_) => false
      case Upstream5xxResponse(_) => true
    } {
      httpClient
        .get(
          urlForCustomsOffices(referenceNumbers = referenceNumbers, countryCodes, roles, activeAt)
        )(using hcWithInternalAuth)
        .execute[List[CustomsOffice]](using throwOnFailure(readEitherOf[List[CustomsOffice]]))
    }
    fetchResult.failed.foreach(err =>
      logger.error(s"Retries exceeded while fetching customs offices", err)
    )
    fetchResult
  }
}
