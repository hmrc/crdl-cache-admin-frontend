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
import uk.gov.hmrc.crdlcacheadminfrontend.models.paging.PagedResult

@Singleton
class CRDLConnector @Inject() (config: AppConfig, httpClient: HttpClientV2)(using
  system: ActorSystem
) extends Retries
  with Logging {
  override protected def actorSystem: ActorSystem = system

  override protected def configuration: Config = config.config.underlying

  private val crdlCacheCodeListsUrl        = s"${config.crdlCacheUrl}/lists"
  private val crdlCacheCodeListsUrlV2      = s"${config.crdlCacheUrl}/v2/lists"
  private val crdlCacheCustomsOfficesUrlV2 = s"${config.crdlCacheUrl}/v2/offices"

  def fetchCodeListSnapShots(pageNum: Int, pageSize: Int)(using
    hc: HeaderCarrier,
    ex: ExecutionContext
  ): Future[PagedResult[CodeListSnapshot]] = {
    logger.info(s"Fetching codelist snapshots from crdl-cache")
    val fetchResult = retryFor(s"fetch of codelist snapshots") {
      // No point in retrying if our request is wrong
      case Upstream4xxResponse(_) => false
      // Attempt to recover from intermittent connectivity issues
      case Upstream5xxResponse(_) => true
    } {
      httpClient
        .get(url"$crdlCacheCodeListsUrlV2?pageNum=$pageNum&pageSize=$pageSize")
        .execute[PagedResult[CodeListSnapshot]](using
          throwOnFailure(readEitherOf[PagedResult[CodeListSnapshot]])
        )
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
    logger.info(s"Fetching ${code} codelist from crdl-cache")
    val fetchResult = retryFor(s"fetch of codelist entries for ${code}") {
      case Upstream4xxResponse(_) => false
      case Upstream5xxResponse(_) => true
    } {
      httpClient
        .get(urlForCodeList(code, filterKeys, filterProperties))
        .execute[List[CodeListEntry]](using throwOnFailure(readEitherOf[List[CodeListEntry]]))
    }
    fetchResult.failed.foreach(err =>
      logger.error(s"Retries exceeded while fetching ${code} ", err)
    )
    fetchResult
  }

  private def urlForCustomsOfficeSummaries(
    pageNum: Int,
    pageSize: Int,
    referenceNumber: Option[String],
    countryCode: Option[String],
    officeName: Option[String]
  ): URL = {
    val filterParams = Seq(
      referenceNumber.map(r => s"referenceNumber=$r"),
      countryCode.map(c => s"countryCode=$c"),
      officeName.map(n => s"officeName=$n")
    ).flatten
    val allParams =
      (Seq(s"pageNum=$pageNum", s"pageSize=$pageSize") ++ filterParams).mkString("&")
    val urlString = s"$crdlCacheCustomsOfficesUrlV2/summaries?$allParams"
    url"$urlString"
  }

  def fetchCustomsOfficeSummaries(
    pageNum: Int,
    pageSize: Int,
    referenceNumber: Option[String] = None,
    countryCode: Option[String] = None,
    officeName: Option[String] = None
  )(using hc: HeaderCarrier, ec: ExecutionContext): Future[PagedResult[CustomsOfficeSummary]] = {
    logger.info(s"Fetching customs office summaries from crdl-cache")
    val fetchResult = retryFor("Fetching customs office summaries") {
      case Upstream4xxResponse(_) => false
      case Upstream5xxResponse(_) => true
    } {
      httpClient
        .get(
          urlForCustomsOfficeSummaries(pageNum, pageSize, referenceNumber, countryCode, officeName)
        )
        .execute[PagedResult[CustomsOfficeSummary]](using
          throwOnFailure(readEitherOf[PagedResult[CustomsOfficeSummary]])
        )
    }
    fetchResult.failed.foreach(err =>
      logger.error(s"Retries exceeded while fetching customs office summaries", err)
    )
    fetchResult
  }

  def fetchCustomsOfficeDetail(
    referenceNumber: String
  )(using hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CustomsOffice]] = {
    val urlString = s"$crdlCacheCustomsOfficesUrlV2/$referenceNumber"
    logger.info(s"Fetching customs office detail for $referenceNumber from crdl-cache")
    val fetchResult = retryFor(s"fetching customs office detail for $referenceNumber") {
      case Upstream4xxResponse(_) => false
      case Upstream5xxResponse(_) => true
    } {
      httpClient
        .get(url"$urlString")
        .execute[Option[CustomsOffice]]
    }
    fetchResult.failed.foreach(err =>
      logger.error(
        s"Retries exceeded while fetching customs office detail for $referenceNumber",
        err
      )
    )
    fetchResult
  }
}
