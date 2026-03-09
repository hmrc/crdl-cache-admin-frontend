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

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.crdlcacheadminfrontend.config.AppConfig
import play.api.Configuration
import org.apache.pekko.actor.ActorSystem
import com.github.tomakehurst.wiremock.client.WireMock.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.stubbing.Scenario
import uk.gov.hmrc.crdlcacheadminfrontend.dataTraits.{
  CustomsOfficeSummaryTestData,
  CodeListSnapShotsTestData
}
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.models.{CustomsOffice, CustomsOfficeDetail}

class CRDLConnectorSpec
  extends AsyncFlatSpec
  with Matchers
  with WireMockSupport
  with HttpClientV2Support
  with CustomsOfficeSummaryTestData
  with CustomsOfficeTestData 
  with CodeListSnapShotsTestData {
    
  given actorSystem: ActorSystem = ActorSystem("test")
  given HeaderCarrier            = HeaderCarrier()

  private val officeSumamriesUrl  = "/crdl-cache/v2/offices/summaries"
  private val officeDetailBaseUrl = "/crdl-cache/v2/offices"
  private val codeListSnapShotsUrl = "/crdl-cache/v2/lists"

  private val appConfig = new AppConfig(
    Configuration(
      "microservice.services.crdl-cache.protocol" -> "http",
      "microservice.services.crdl-cache.host"     -> "localhost",
      "microservice.services.crdl-cache.port"     -> wireMockPort,
      "http-verbs.retries.intervals"              -> List("1.millis")
    )
  )

  private val connector = new CRDLConnector(
    appConfig,
    httpClientV2
  )

  private val retryScenario = "Retry"
  private val failedState   = "Failed"

  def customsOfficeSummariesShouldError(errorResponse: () => ResponseDefinitionBuilder) = {
    stubFor(
      get(urlPathEqualTo(officeSumamriesUrl))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .willReturn(errorResponse())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector.fetchCustomsOfficeSummaries(1, 10)
    }
  }

  def fetchCodeListSnapShotsError(errorResponse: () => ResponseDefinitionBuilder) = {
    stubFor(
      get(urlPathEqualTo(codeListSnapShotsUrl))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .willReturn(errorResponse())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector.fetchCodeListSnapShots(1, 10)
    }
  }

  def customsOfficeSummariesTestRetry(
    errorResponse: () => ResponseDefinitionBuilder,
    shouldRetry: Boolean
  ) = {
    stubFor(
      get(urlEqualTo(s"$officeSumamriesUrl?pageNum=1&pageSize=10"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(errorResponse())
        .willSetStateTo(failedState)
    )

    stubFor(
      get(urlEqualTo(s"$officeSumamriesUrl?pageNum=1&pageSize=10"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .willReturn(ok().withBody(asJson(pagedCustomsOfficeSummaryResult)))
    )

    if (shouldRetry) {
      connector
        .fetchCustomsOfficeSummaries(1, 10)
        .map(_ mustBe pagedCustomsOfficeSummaryResult)
    } else {
      recoverToSucceededIf[UpstreamErrorResponse] {
        connector.fetchCustomsOfficeSummaries(1, 10)
      }
    }
  }

  def fetchCodeListSnapShotsTestRetry(
    errorResponse: () => ResponseDefinitionBuilder,
    shouldRetry: Boolean
  ) = {
    stubFor(
      get(urlEqualTo(s"$codeListSnapShotsUrl?pageNum=1&pageSize=10"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(errorResponse())
        .willSetStateTo(failedState)
    )

    stubFor(
      get(urlEqualTo(s"$codeListSnapShotsUrl?pageNum=1&pageSize=10"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .willReturn(ok().withBody(asJson(pagedCodeListSnapShotResult)))
    )

    if (shouldRetry) {
      connector
        .fetchCodeListSnapShots(1, 10)
        .map(_ mustBe pagedCodeListSnapShotResult)
    } else {
      recoverToSucceededIf[UpstreamErrorResponse] {
        connector.fetchCodeListSnapShots(1, 10)
      }
    }
  }

  "CRDLConnector.fetchCustomsOfficeSummaries: return the data as delivered from the API" should "return the data as delivered from the API" in {
    val expectedResult = pagedCustomsOfficeSummaryResult
    val pageNum        = 1
    val pageSize       = pagedCustomsOfficeSummaryResult.items.length

    stubFor(
      get(urlPathEqualTo(officeSumamriesUrl))
        .withQueryParam("pageNum", equalTo(s"$pageNum"))
        .withQueryParam("pageSize", equalTo(s"$pageSize"))
        .willReturn(
          ok().withBody(asJson(pagedCustomsOfficeSummaryResult))
        )
    )

    connector
      .fetchCustomsOfficeSummaries(pageNum, pageSize)
      .map(_ mustBe expectedResult)
  }

  "CRDLConnector.fetchCodeListSnapShots: return the data as delivered from the API" should "return the data as delivered from the API" in {
    val expectedResult = pagedCodeListSnapShotResult
    val pageNum        = 1
    val pageSize       = pagedCodeListSnapShotResult.items.length

    stubFor(
      get(urlPathEqualTo(codeListSnapShotsUrl))
        .withQueryParam("pageNum", equalTo(s"$pageNum"))
        .withQueryParam("pageSize", equalTo(s"$pageSize"))
        .willReturn(
          ok().withBody(asJson(pagedCodeListSnapShotResult))
        )
    )

    connector
      .fetchCodeListSnapShots(pageNum, pageSize)
      .map(_ mustBe expectedResult)
  }

  it should "throw UpstreamErrorResponse when a client error is returned" in {
    customsOfficeSummariesShouldError(badRequest)
  }

  it should "throw UpstreamErrorResponse when a server error is returned by the API" in {
    customsOfficeSummariesShouldError(serverError)
  }

  it should "should not Retry when a client error is returned" in {
    customsOfficeSummariesTestRetry(badRequest, false)
  }

  it should "should Retry when a server error is returned from the API" in {
    customsOfficeSummariesTestRetry(serverError, true)
  }

  it should "include referenceNumber as a query param when provided" in {
    stubFor(
      get(urlPathEqualTo(officeSumamriesUrl))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .withQueryParam("referenceNumber", equalTo("GB000001"))
        .willReturn(ok().withBody(asJson(pagedCustomsOfficeSummaryResult)))
    )

    connector
      .fetchCustomsOfficeSummaries(1, 10, referenceNumber = Some("GB000001"))
      .map(_ mustBe pagedCustomsOfficeSummaryResult)
  }

  it should "include countryCode as a query param when provided" in {
    stubFor(
      get(urlPathEqualTo(officeSumamriesUrl))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .withQueryParam("countryCode", equalTo("GB"))
        .willReturn(ok().withBody(asJson(pagedCustomsOfficeSummaryResult)))
    )

    connector
      .fetchCustomsOfficeSummaries(1, 10, countryCode = Some("GB"))
      .map(_ mustBe pagedCustomsOfficeSummaryResult)
  }

  it should "include officeName as a query param when provided" in {
    stubFor(
      get(urlPathEqualTo(officeSumamriesUrl))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .withQueryParam("officeName", equalTo("London Office"))
        .willReturn(ok().withBody(asJson(pagedCustomsOfficeSummaryResult)))
    )

    connector
      .fetchCustomsOfficeSummaries(1, 10, officeName = Some("London Office"))
      .map(_ mustBe pagedCustomsOfficeSummaryResult)
  }

  it should "include all filter params as query params when all are provided" in {
    stubFor(
      get(urlPathEqualTo(officeSumamriesUrl))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .withQueryParam("referenceNumber", equalTo("GB000001"))
        .withQueryParam("countryCode", equalTo("GB"))
        .withQueryParam("officeName", equalTo("London Office"))
        .willReturn(ok().withBody(asJson(pagedCustomsOfficeSummaryResult)))
    )

    connector
      .fetchCustomsOfficeSummaries(1, 10, Some("GB000001"), Some("GB"), Some("London Office"))
      .map(_ mustBe pagedCustomsOfficeSummaryResult)
  }

  // fetchCustomsOfficeDetail

  private val officeDetailReferenceNumber = "GB000001"
  private val officeDetailUrl             = s"$officeDetailBaseUrl/$officeDetailReferenceNumber"

  private val officeDetailJson =
    """{
          |  "referenceNumber": "GB000001",
          |  "countryCode": "GB",
          |  "postalCode": "SW1A 1AA",
          |  "traderDedicated": false,
          |  "customsOfficeSpecificNotesCodes": [],
          |  "customsOfficeLsd": {
          |    "customsOfficeUsualName": "London Office",
          |    "languageCode": "EN",
          |    "city": "London",
          |    "prefixSuffixFlag": false,
          |    "spaceToAdd": false,
          |    "streetAndNumber": "1 Test Street"
          |  },
          |  "customsOfficeTimetable": []
          |}""".stripMargin

  private val expectedOfficeDetail = CustomsOffice(
    referenceNumber = "GB000001",
    activeFrom = None,
    activeTo = None,
    referenceNumberMainOffice = None,
    referenceNumberHigherAuthority = None,
    referenceNumberCompetentAuthorityOfEnquiry = None,
    referenceNumberCompetentAuthorityOfRecovery = None,
    referenceNumberTakeover = None,
    countryCode = "GB",
    emailAddress = None,
    unLocodeId = None,
    nctsEntryDate = None,
    nearestOffice = None,
    postalCode = "SW1A 1AA",
    phoneNumber = None,
    faxNumber = None,
    telexNumber = None,
    geoInfoCode = None,
    regionCode = None,
    traderDedicated = false,
    dedicatedTraderLanguageCode = None,
    dedicatedTraderName = None,
    customsOfficeSpecificNotesCodes = List(),
    customsOfficeLsd = CustomsOfficeDetail(
      customsOfficeUsualName = "London Office",
      languageCode = "EN",
      city = "London",
      prefixSuffixFlag = false,
      prefixSuffixLevel = None,
      prefixSuffixName = None,
      spaceToAdd = false,
      streetAndNumber = "1 Test Street"
    ),
    customsOfficeTimetable = List()
  )

  def officeDetailShouldError(errorResponse: () => ResponseDefinitionBuilder) =
    stubFor(
      get(urlEqualTo(officeDetailUrl))
        .willReturn(errorResponse())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      connector.fetchCustomsOfficeDetail(officeDetailReferenceNumber)
    }

  def officeDetailTestRetry(errorResponse: () => ResponseDefinitionBuilder, shouldRetry: Boolean) =
    stubFor(
      get(urlEqualTo(officeDetailUrl))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(errorResponse())
        .willSetStateTo(failedState)
    )

    stubFor(
      get(urlEqualTo(officeDetailUrl))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .willReturn(ok().withBody(officeDetailJson))
    )

    if (shouldRetry)
      connector
        .fetchCustomsOfficeDetail(officeDetailReferenceNumber)
        .map(_ mustBe Some(expectedOfficeDetail))
    else
      recoverToSucceededIf[UpstreamErrorResponse] {
        connector.fetchCustomsOfficeDetail(officeDetailReferenceNumber)
      }

  "CRDLConnector.fetchCustomsOfficeDetail" should "return Some(office) when the office is found" in {
    stubFor(
      get(urlEqualTo(officeDetailUrl))
        .willReturn(ok().withBody(officeDetailJson))
    )

    connector
      .fetchCustomsOfficeDetail(officeDetailReferenceNumber)
      .map(_ mustBe Some(expectedOfficeDetail))
  }

  it should "return None when the office is not found (404)" in {
    stubFor(
      get(urlEqualTo(officeDetailUrl))
        .willReturn(notFound())
    )

    connector
      .fetchCustomsOfficeDetail(officeDetailReferenceNumber)
      .map(_ mustBe None)
  }

  it should "throw UpstreamErrorResponse when a client error is returned" in {
    officeDetailShouldError(badRequest)
  }

  it should "throw UpstreamErrorResponse when a server error is returned" in {
    officeDetailShouldError(serverError)
  }

  it should "not retry when a client error is returned" in {
    officeDetailTestRetry(badRequest, false)
  }

  it should "retry when a server error is returned" in {
    officeDetailTestRetry(serverError, true)
  it should "throw UpstreamErrorResponse when a client error is returned for fetchCodeListSnapShots" in {
    fetchCodeListSnapShotsError(badRequest)
  }

  it should "throw UpstreamErrorResponse when a server error is returned for fetchCodeListSnapShots" in {
    fetchCodeListSnapShotsError(serverError)
  }

  it should "should not Retry when a client error is returned for fetchCodeListSnapShots" in {
    fetchCodeListSnapShotsTestRetry(badRequest, false)
  }

  it should "should Retry when a server error is returned from for fetchCodeListSnapShots" in {
    fetchCodeListSnapShotsTestRetry(serverError, true)
  }
}
