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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.apache.pekko.actor.ActorSystem
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.Configuration
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import uk.gov.hmrc.crdlcacheadminfrontend.codeLists.models.{CodeListEntry, CodeListSnapshot}
import uk.gov.hmrc.crdlcacheadminfrontend.config.AppConfig
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.models.{CustomsOffice, CustomsOfficeDetail, CustomsOfficeSummary}
import uk.gov.hmrc.crdlcacheadminfrontend.models.paging.PagedResult
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.util.UUID
import uk.gov.hmrc.http.Authorization

import java.time.Instant

class CRDLConnectorSpec
  extends AsyncFlatSpec
  with Matchers
  with WireMockSupport
  with HttpClientV2Support {
  given ActorSystem = ActorSystem("test")

  given HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(authToken)))

  private val authToken = UUID.randomUUID().toString

  private val appConfig = new AppConfig(
    Configuration(
      "microservice.services.crdl-cache.host" -> "localhost",
      "microservice.services.crdl-cache.port" -> wireMockPort,
      "http-verbs.retries.intervals" -> List("1.millis")
    )
  )

  override lazy val wireMockRootDirectory = "it/test/resources"

  private val crdlConnector = new CRDLConnector(appConfig, httpClientV2)

  "CRDLConnector.fetchCodeList" should "fetch codelist entries when no filtering parameters are provided" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/lists/BC36"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC36-nofilter.json")
        )
    )

    val expected = List(
      CodeListEntry(
        "E600",
        "Saturated acyclic hydrocarbons Products falling within CN code 2901 10",
        Json.obj(
          "unitOfMeasureCode" -> "1",
          "degreePlatoApplicabilityFlag" -> false,
          "actionIdentification" -> "1103",
          "exciseProductsCategoryCode" -> "E",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag" -> false
        )
      ),
      CodeListEntry(
        "S200",
        "Spirituous beverages",
        Json.obj(
          "unitOfMeasureCode" -> "3",
          "degreePlatoApplicabilityFlag" -> false,
          "actionIdentification" -> "1110",
          "exciseProductsCategoryCode" -> "S",
          "alcoholicStrengthApplicabilityFlag" -> true,
          "densityApplicabilityFlag" -> false
        )
      ),
      CodeListEntry(
        "T300",
        "Cigars & cigarillos",
        Json.obj(
          "unitOfMeasureCode" -> "4",
          "degreePlatoApplicabilityFlag" -> false,
          "actionIdentification" -> "1116",
          "exciseProductsCategoryCode" -> "T",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag" -> false
        )
      )
    )

    crdlConnector
      .fetchCodeList("BC36")
      .map {
        _ shouldBe expected
      }
  }

  it should "fetch codelist entries when filtering by keys" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/lists/BC36"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .withQueryParam("keys", equalTo("E600,T300"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC36-filterkeys.json")
        )
    )

    val expected = List(
      CodeListEntry(
        "E600",
        "Saturated acyclic hydrocarbons Products falling within CN code 2901 10",
        Json.obj(
          "unitOfMeasureCode" -> "1",
          "degreePlatoApplicabilityFlag" -> false,
          "actionIdentification" -> "1103",
          "exciseProductsCategoryCode" -> "E",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag" -> false
        )
      ),
      CodeListEntry(
        "T300",
        "Cigars & cigarillos",
        Json.obj(
          "unitOfMeasureCode" -> "4",
          "degreePlatoApplicabilityFlag" -> false,
          "actionIdentification" -> "1116",
          "exciseProductsCategoryCode" -> "T",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag" -> false
        )
      )
    )

    crdlConnector
      .fetchCodeList(
        "BC36",
        filterKeys = Some(Set("E600", "T300"))
      )
      .map {
        _ shouldBe expected
      }
  }

  it should "fetch codelist entries when filtering by properties" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/lists/BC36"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .withQueryParam("alcoholicStrengthApplicabilityFlag", equalTo("true"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC36-filterproperties.json")
        )
    )

    val expected = List(
      CodeListEntry(
        "S200",
        "Spirituous beverages",
        Json.obj(
          "unitOfMeasureCode" -> "3",
          "degreePlatoApplicabilityFlag" -> false,
          "actionIdentification" -> "1110",
          "exciseProductsCategoryCode" -> "S",
          "alcoholicStrengthApplicabilityFlag" -> true,
          "densityApplicabilityFlag" -> false
        )
      )
    )

    crdlConnector
      .fetchCodeList(
        "BC36",
        filterProperties = Some(Map("alcoholicStrengthApplicabilityFlag" -> true))
      )
      .map {
        _ shouldBe expected
      }
  }

  it should "fetch codelist entries when filtering by phase and domain" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/lists/CL017"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .withQueryParam("phase", equalTo("P6"))
        .withQueryParam("domain", equalTo("NCTS"))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/CL017.json")
        )
    )

    val expected = List(
      CodeListEntry("ZU", "Intermediate bulk container, flexible", Json.obj("state" -> "valid")),
      CodeListEntry(
        "ZV",
        "Intermediate bulk container, metal, other than steel",
        Json.obj("state" -> "valid")
      ),
      CodeListEntry(
        "ZW",
        "Intermediate bulk container, natural wood",
        Json.obj("state" -> "valid")
      ),
      CodeListEntry("ZX", "Intermediate bulk container, plywood", Json.obj("state" -> "valid")),
      CodeListEntry(
        "ZY",
        "Intermediate bulk container, reconstituted wood",
        Json.obj("state" -> "valid")
      ),
      CodeListEntry("ZZ", "Mutually defined", Json.obj("state" -> "valid"))
    )

    crdlConnector
      .fetchCodeList(
        "CL017",
        phase = Some("P6"),
        domain = Some("NCTS")
      )
      .map {
        _ shouldBe expected
      }
  }

  it should "throw UpstreamErrorResponse when crdl-cache returns a client error" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/lists/BC999"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(badRequest())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCodeList("BC999")
    }
  }

  it should "throw UpstreamErrorResponse when crdl-cache returns a server error consistently" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/lists/BC36"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(serverError())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCodeList("BC36")
    }
  }

  it should "not retry when crdl-cache returns a client error" in {
    val retryScenario = "Retry"
    val failedState = "Failed"

    stubFor(
      get(urlPathEqualTo("/crdl-cache/lists/BC999"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(badRequest())
        .willSetStateTo(failedState)
    )

    // Queue up a success response for the second call, which should never happen
    stubFor(
      get(urlPathEqualTo("/crdl-cache/lists/BC999"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC36-nofilter.json")
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCodeList("BC999")
    }.map { assertion =>
      verify(1, getRequestedFor(urlPathEqualTo("/crdl-cache/lists/BC999")))
      assertion
    }
  }

  it should "retry when crdl-cache returns a server error" in {
    val retryScenario = "Retry"
    val failedState = "Failed"

    stubFor(
      get(urlPathEqualTo("/crdl-cache/lists/BC36"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(serverError())
        .willSetStateTo(failedState)
    )

    // Queue up a success response for the retry
    stubFor(
      get(urlPathEqualTo("/crdl-cache/lists/BC36"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/BC36-nofilter.json")
        )
    )

    val expected = List(
      CodeListEntry(
        "E600",
        "Saturated acyclic hydrocarbons Products falling within CN code 2901 10",
        Json.obj(
          "unitOfMeasureCode" -> "1",
          "degreePlatoApplicabilityFlag" -> false,
          "actionIdentification" -> "1103",
          "exciseProductsCategoryCode" -> "E",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag" -> false
        )
      ),
      CodeListEntry(
        "S200",
        "Spirituous beverages",
        Json.obj(
          "unitOfMeasureCode" -> "3",
          "degreePlatoApplicabilityFlag" -> false,
          "actionIdentification" -> "1110",
          "exciseProductsCategoryCode" -> "S",
          "alcoholicStrengthApplicabilityFlag" -> true,
          "densityApplicabilityFlag" -> false
        )
      ),
      CodeListEntry(
        "T300",
        "Cigars & cigarillos",
        Json.obj(
          "unitOfMeasureCode" -> "4",
          "degreePlatoApplicabilityFlag" -> false,
          "actionIdentification" -> "1116",
          "exciseProductsCategoryCode" -> "T",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag" -> false
        )
      )
    )

    crdlConnector
      .fetchCodeList("BC36")
      .map { result =>
        verify(2, getRequestedFor(urlPathEqualTo("/crdl-cache/lists/BC36")))
        result shouldBe expected
      }
  }

  "CRDLConnector.fetchCodeListSnapShots" should "fetch codelist snapshots when no filtering parameters are provided" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/lists"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("4"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/snapshots-response.json")
        )
    )

    val expected = PagedResult(
      items = List(
        CodeListSnapshot(
          codeListCode = "BC46",
          snapshotVersion = 1,
          phase = None,
          domain = None,
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "BC57",
          snapshotVersion = 1,
          phase = None,
          domain = None,
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "CL008",
          snapshotVersion = 1,
          phase = Some("P6"),
          domain = Some("NCTS"),
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "CL042",
          snapshotVersion = 1,
          phase = Some("P6"),
          domain = Some("NCTS"),
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        )
      ),
      pageNum = 1,
      pageSize = 4,
      itemsInPage = 4,
      totalItems = 4,
      totalPages = 1
    )

    crdlConnector
      .fetchCodeListSnapShots(pageNum = 1, pageSize = 4)
      .map {
        _ shouldBe expected
      }
  }

  it should "fetch entries when phase and domain are both provided" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/lists"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("4"))
        .withQueryParam("phase", equalTo("P6"))
        .withQueryParam("domain", equalTo("NCTS"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/snapshots-response.json")
        )
    )

    val expected = PagedResult(
      items = List(
        CodeListSnapshot(
          codeListCode = "BC46",
          snapshotVersion = 1,
          phase = None,
          domain = None,
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "BC57",
          snapshotVersion = 1,
          phase = None,
          domain = None,
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "CL008",
          snapshotVersion = 1,
          phase = Some("P6"),
          domain = Some("NCTS"),
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "CL042",
          snapshotVersion = 1,
          phase = Some("P6"),
          domain = Some("NCTS"),
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        )
      ),
      pageNum = 1,
      pageSize = 4,
      itemsInPage = 4,
      totalItems = 4,
      totalPages = 1
    )

    crdlConnector
      .fetchCodeListSnapShots(pageNum = 1, pageSize = 4, phase = Some("P6"), domain = Some("NCTS"))
      .map {
        _ shouldBe expected
      }
  }

  it should "fetch entries when codeListCode is provided" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/lists"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("4"))
        .withQueryParam("codeListCode", equalTo("BC46"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/snapshots-response.json")
        )
    )

    val expected = PagedResult(
      items = List(
        CodeListSnapshot(
          codeListCode = "BC46",
          snapshotVersion = 1,
          phase = None,
          domain = None,
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "BC57",
          snapshotVersion = 1,
          phase = None,
          domain = None,
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "CL008",
          snapshotVersion = 1,
          phase = Some("P6"),
          domain = Some("NCTS"),
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "CL042",
          snapshotVersion = 1,
          phase = Some("P6"),
          domain = Some("NCTS"),
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        )
      ),
      pageNum = 1,
      pageSize = 4,
      itemsInPage = 4,
      totalItems = 4,
      totalPages = 1
    )

    crdlConnector
      .fetchCodeListSnapShots(pageNum = 1, pageSize = 4, codeListCode = Some("BC46"))
      .map {
        _ shouldBe expected
      }
  }

  it should "fetch entries when all parameters are provided" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/lists"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("4"))
        .withQueryParam("codeListCode", equalTo("CL042"))
        .withQueryParam("phase", equalTo("P6"))
        .withQueryParam("domain", equalTo("NCTS"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/snapshots-response.json")
        )
    )

    val expected = PagedResult(
      items = List(
        CodeListSnapshot(
          codeListCode = "BC46",
          snapshotVersion = 1,
          phase = None,
          domain = None,
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "BC57",
          snapshotVersion = 1,
          phase = None,
          domain = None,
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "CL008",
          snapshotVersion = 1,
          phase = Some("P6"),
          domain = Some("NCTS"),
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "CL042",
          snapshotVersion = 1,
          phase = Some("P6"),
          domain = Some("NCTS"),
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        )
      ),
      pageNum = 1,
      pageSize = 4,
      itemsInPage = 4,
      totalItems = 4,
      totalPages = 1
    )

    crdlConnector
      .fetchCodeListSnapShots(
        pageNum = 1,
        pageSize = 4,
        codeListCode = Some("CL042"),
        phase = Some("P6"),
        domain = Some("NCTS")
      )
      .map {
        _ shouldBe expected
      }
  }

  it should "throw UpstreamErrorResponse when crdl-cache returns a client error" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/lists"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("4"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(badRequest())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCodeListSnapShots(pageNum = 1, pageSize = 4)
    }
  }

  it should "throw UpstreamErrorResponse when crdl-cache returns a server error consistently" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/lists"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("4"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(serverError())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCodeListSnapShots(pageNum = 1, pageSize = 4)
    }
  }

  it should "not retry when crdl-cache returns a client error" in {
    val retryScenario = "Retry"
    val failedState = "Failed"

    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/lists"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("4"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(badRequest())
        .willSetStateTo(failedState)
    )

    // Queue up a success response for the second call, which should never happen
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/lists/"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("4"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/snapshots-response.json")
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCodeListSnapShots(pageNum = 1, pageSize = 4)
    }.map { assertion =>
      verify(1, getRequestedFor(urlPathEqualTo("/crdl-cache/admin/lists"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("4")))
      assertion
    }
  }

  it should "retry when crdl-cache returns a server error" in {
    val retryScenario = "Retry"
    val failedState = "Failed"

    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/lists"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("4"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(serverError())
        .willSetStateTo(failedState)
    )

    // Queue up a success response for the retry
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/lists"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("4"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/snapshots-response.json")
        )
    )

    val expected = PagedResult(
      items = List(
        CodeListSnapshot(
          codeListCode = "BC46",
          snapshotVersion = 1,
          phase = None,
          domain = None,
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "BC57",
          snapshotVersion = 1,
          phase = None,
          domain = None,
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "CL008",
          snapshotVersion = 1,
          phase = Some("P6"),
          domain = Some("NCTS"),
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        ),
        CodeListSnapshot(
          codeListCode = "CL042",
          snapshotVersion = 1,
          phase = Some("P6"),
          domain = Some("NCTS"),
          lastUpdated = Some(Instant.parse("2026-06-18T10:00:00Z"))
        )
      ),
      pageNum = 1,
      pageSize = 4,
      itemsInPage = 4,
      totalItems = 4,
      totalPages = 1
    )

    crdlConnector
      .fetchCodeListSnapShots(pageNum = 1, pageSize = 4)
      .map { result =>
        verify(2, getRequestedFor(urlPathEqualTo("/crdl-cache/admin/lists"))
          .withQueryParam("pageNum", equalTo("1"))
          .withQueryParam("pageSize", equalTo("4")))
        result shouldBe expected
      }
  }

  "CRDLConnector.fetchCodeListSnapShot" should "fetch codelist snapshot when phase and domain are not present" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/snapshot/BC109"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/snapshot-BC109.json")
        )
    )

    val expected = Some(
      CodeListSnapshot(
        codeListCode = "BC109",
        snapshotVersion = 2,
        phase = None,
        domain = None,
        lastUpdated = Some(Instant.parse("2025-11-19T11:00:28.295Z"))
      )
    )

    crdlConnector
      .fetchCodeListSnapShot("BC109")
      .map {
        _ shouldBe expected
      }
  }

  it should "fetch codelist snapshot when phase and domain are present" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/snapshot/CL219"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/snapshot-CL219.json")
        )
    )

    val expected = Some(
      CodeListSnapshot(
        codeListCode = "CL219",
        snapshotVersion = 16,
        phase = Some("P6"),
        domain = Some("NCTS"),
        lastUpdated = Some(Instant.parse("2026-06-19T02:30:31.885Z"))
      )
    )

    crdlConnector
      .fetchCodeListSnapShot("CL219")
      .map {
        _ shouldBe expected
      }
  }

  it should "throw UpstreamErrorResponse when crdl-cache returns a client error" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/snapshot/CL219"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(badRequest())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCodeListSnapShot("CL219")
    }
  }

  it should "throw UpstreamErrorResponse when crdl-cache returns a server error consistently" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/lists"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(serverError())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCodeListSnapShot("CL219")
    }
  }

  it should "not retry when crdl-cache returns a client error" in {
    val retryScenario = "Retry"
    val failedState = "Failed"

    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/snapshot/CL219"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(badRequest())
        .willSetStateTo(failedState)
    )

    // Queue up a success response for the second call, which should never happen
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/snapshot/CL219"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/snapshot-CL219.json")
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCodeListSnapShot("CL219")
    }.map { assertion =>
      verify(1, getRequestedFor(urlPathEqualTo("/crdl-cache/admin/snapshot/CL219")))
      assertion
    }
  }

  it should "retry when crdl-cache returns a server error" in {
    val retryScenario = "Retry"
    val failedState = "Failed"

    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/snapshot/CL219"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(serverError())
        .willSetStateTo(failedState)
    )

    // Queue up a success response for the retry
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/snapshot/CL219"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))

        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("codelist/snapshot-CL219.json")
        )
    )

    val expected = Some(
      CodeListSnapshot(
        codeListCode = "CL219",
        snapshotVersion = 16,
        phase = Some("P6"),
        domain = Some("NCTS"),
        lastUpdated = Some(Instant.parse("2026-06-19T02:30:31.885Z"))
      )
    )

    crdlConnector
      .fetchCodeListSnapShot("CL219")
      .map { result =>
        verify(2, getRequestedFor(urlPathEqualTo("/crdl-cache/admin/snapshot/CL219")))
        result shouldBe expected
      }

  }

  it should "return None when the code list snapshot is not found" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/snapshot/UNKNOWN"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(notFound())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector
        .fetchCodeListSnapShot("UNKNOWN")
    }
  }

  "CRDLConnector.fetchCustomsOfficeSummaries" should "fetch customs office summaries when no filtering parameters are provided" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/summaries"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/customs-office-summary-response.json")
        )
    )

    val expected = PagedResult(
      items = List(
        CustomsOfficeSummary(
          referenceNumber = "GB000060",
          countryCode = "GB",
          customsOfficeUsualName = "Dover",
          phase = Some("P6"),
          domain = Some("NCTS")
        )
      ),
      pageNum = 1,
      pageSize = 10,
      itemsInPage = 1,
      totalItems = 1,
      totalPages = 1
    )

    crdlConnector
      .fetchCustomsOfficeSummaries(pageNum = 1, pageSize = 10)
      .map {
        _ shouldBe expected
      }
  }

  it should "fetch entries when reference number is provided" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/summaries"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .withQueryParam("referenceNumber", equalTo("GB000060"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/customs-office-summary-response.json")
        )
    )

    val expected = PagedResult(
      items = List(
        CustomsOfficeSummary(
          referenceNumber = "GB000060",
          countryCode = "GB",
          customsOfficeUsualName = "Dover",
          phase = Some("P6"),
          domain = Some("NCTS")
        )
      ),
      pageNum = 1,
      pageSize = 10,
      itemsInPage = 1,
      totalItems = 1,
      totalPages = 1
    )

    crdlConnector
      .fetchCustomsOfficeSummaries(pageNum = 1, pageSize = 10, referenceNumber = Some("GB000060"))
      .map {
        _ shouldBe expected
      }
  }

  it should "fetch entries when country code is provided" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/summaries"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .withQueryParam("countryCode", equalTo("GB"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/customs-office-summary-response.json")
        )
    )

    val expected = PagedResult(
      items = List(
        CustomsOfficeSummary(
          referenceNumber = "GB000060",
          countryCode = "GB",
          customsOfficeUsualName = "Dover",
          phase = Some("P6"),
          domain = Some("NCTS")
        )
      ),
      pageNum = 1,
      pageSize = 10,
      itemsInPage = 1,
      totalItems = 1,
      totalPages = 1
    )

    crdlConnector
      .fetchCustomsOfficeSummaries(pageNum = 1, pageSize = 10, countryCode = Some("GB"))
      .map {
        _ shouldBe expected
      }
  }

  it should "fetch entries when customs office usual name is provided" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/summaries"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .withQueryParam("officeName", equalTo("Dover"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/customs-office-summary-response.json")
        )
    )

    val expected = PagedResult(
      items = List(
        CustomsOfficeSummary(
          referenceNumber = "GB000060",
          countryCode = "GB",
          customsOfficeUsualName = "Dover",
          phase = Some("P6"),
          domain = Some("NCTS")
        )
      ),
      pageNum = 1,
      pageSize = 10,
      itemsInPage = 1,
      totalItems = 1,
      totalPages = 1
    )

    crdlConnector
      .fetchCustomsOfficeSummaries(pageNum = 1, pageSize = 10, officeName = Some("Dover"))
      .map {
        _ shouldBe expected
      }
  }

  it should "throw UpstreamErrorResponse when crdl-cache returns a client error" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/office/summaries"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(badRequest())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCustomsOfficeSummaries(pageNum = 1, pageSize = 10)
    }
  }

  it should "throw UpstreamErrorResponse when crdl-cache returns a server error consistently" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/summaries"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(serverError())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCustomsOfficeSummaries(pageNum = 1, pageSize = 10)
    }
  }

  it should "not retry when crdl-cache returns a client error" in {
    val retryScenario = "Retry"
    val failedState = "Failed"

    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/summaries"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(badRequest())
        .willSetStateTo(failedState)
    )

    // Queue up a success response for the second call, which should never happen
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/summaries"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/customs-office-summary-response.json")
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCustomsOfficeSummaries(pageNum = 1, pageSize = 10)
    }.map { assertion =>
      verify(1, getRequestedFor(urlPathEqualTo("/crdl-cache/admin/offices/summaries"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10")))
      assertion
    }

  }

  it should "retry when crdl-cache returns a server error" in {
    val retryScenario = "Retry"
    val failedState = "Failed"

    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/summaries"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(serverError())
        .willSetStateTo(failedState)
    )

    // Queue up a success response for the retry
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/summaries"))
        .withQueryParam("pageNum", equalTo("1"))
        .withQueryParam("pageSize", equalTo("10"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/customs-office-summary-response.json")
        )
    )

    val expected = PagedResult(
      items = List(
        CustomsOfficeSummary(
          referenceNumber = "GB000060",
          countryCode = "GB",
          customsOfficeUsualName = "Dover",
          phase = Some("P6"),
          domain = Some("NCTS")
        )
      ),
      pageNum = 1,
      pageSize = 10,
      itemsInPage = 1,
      totalItems = 1,
      totalPages = 1
    )

    crdlConnector
      .fetchCustomsOfficeSummaries(pageNum = 1, pageSize = 10)
      .map { result =>
        verify(2, getRequestedFor(urlPathEqualTo("/crdl-cache/admin/offices/summaries"))
          .withQueryParam("pageNum", equalTo("1"))
          .withQueryParam("pageSize", equalTo("10")))
        result shouldBe expected
      }
  }

  "CRDLConnector.fetchCustomsOfficeDetail" should "fetch customs office details" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/GB000001"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/customs-office-detail-response.json")
        )
    )

    val expected = Some(
      CustomsOffice(
        referenceNumber = "GB000001",
        phase = Some("P6"),
        domain = Some("NCTS"),
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
    )

    crdlConnector
      .fetchCustomsOfficeDetail(referenceNumber = "GB000001")
      .map {
        _ shouldBe expected
      }
  }

  it should "throw UpstreamErrorResponse when crdl-cache returns a client error" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/GB000001"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(badRequest())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCustomsOfficeDetail(referenceNumber = "GB000001")
    }
  }

  it should "throw UpstreamErrorResponse when crdl-cache returns a server error consistently" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/GB000001"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(serverError())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCustomsOfficeDetail(referenceNumber = "GB000001")
    }
  }

  it should "not retry when crdl-cache returns a client error" in {
    val retryScenario = "Retry"
    val failedState = "Failed"

    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/GB000001"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(badRequest())
        .willSetStateTo(failedState)
    )

    // Queue up a success response for the second call, which should never happen
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/GB000001"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/customs-office-detail-response.json")
        )
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector.fetchCustomsOfficeDetail(referenceNumber = "GB000001")
    }.map { assertion =>
      verify(1, getRequestedFor(urlPathEqualTo("/crdl-cache/admin/offices/GB000001")))
      assertion
    }

  }

  it should "retry when crdl-cache returns a server error" in {
    val retryScenario = "Retry"
    val failedState = "Failed"

    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/GB000001"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(Scenario.STARTED)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(serverError())
        .willSetStateTo(failedState)
    )

    // Queue up a success response for the retry
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/GB000001"))
        .inScenario(retryScenario)
        .whenScenarioStateIs(failedState)
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(
          ok()
            .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
            .withBodyFile("col/customs-office-detail-response.json")
        )
    )


    val expected = Some(
      CustomsOffice(
        referenceNumber = "GB000001",
        phase = Some("P6"),
        domain = Some("NCTS"),
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
    )

    crdlConnector
      .fetchCustomsOfficeDetail(referenceNumber = "GB000001")
      .map { result =>
        verify(2, getRequestedFor(urlPathEqualTo("/crdl-cache/admin/offices/GB000001")))
        result shouldBe expected
      }
  }

  it should "return None when the customs office is not found" in {
    stubFor(
      get(urlPathEqualTo("/crdl-cache/admin/offices/UNKNOWN"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(authToken))
        .willReturn(notFound())
    )

    recoverToSucceededIf[UpstreamErrorResponse] {
      crdlConnector
        .fetchCodeListSnapShot("UNKNOWN")
    }

  }
}