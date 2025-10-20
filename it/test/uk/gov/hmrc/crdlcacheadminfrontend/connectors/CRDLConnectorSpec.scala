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
import uk.gov.hmrc.crdlcacheadminfrontend.codeLists.models.CodeListEntry
import uk.gov.hmrc.crdlcacheadminfrontend.config.AppConfig
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.util.UUID
import uk.gov.hmrc.http.Authorization

class CRDLConnectorSpec
  extends AsyncFlatSpec
  with Matchers
  with WireMockSupport
  with HttpClientV2Support {
  given ActorSystem   = ActorSystem("test")
  given HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(authToken)))

  private val authToken = UUID.randomUUID().toString

  private val appConfig = new AppConfig(
    Configuration(
      "microservice.services.crdl-cache.host" -> "localhost",
      "microservice.services.crdl-cache.port" -> wireMockPort,
      "http-verbs.retries.intervals"          -> List("1.millis")
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
          "unitOfMeasureCode"                  -> "1",
          "degreePlatoApplicabilityFlag"       -> false,
          "actionIdentification"               -> "1103",
          "exciseProductsCategoryCode"         -> "E",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag"           -> false
        )
      ),
      CodeListEntry(
        "S200",
        "Spirituous beverages",
        Json.obj(
          "unitOfMeasureCode"                  -> "3",
          "degreePlatoApplicabilityFlag"       -> false,
          "actionIdentification"               -> "1110",
          "exciseProductsCategoryCode"         -> "S",
          "alcoholicStrengthApplicabilityFlag" -> true,
          "densityApplicabilityFlag"           -> false
        )
      ),
      CodeListEntry(
        "T300",
        "Cigars & cigarillos",
        Json.obj(
          "unitOfMeasureCode"                  -> "4",
          "degreePlatoApplicabilityFlag"       -> false,
          "actionIdentification"               -> "1116",
          "exciseProductsCategoryCode"         -> "T",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag"           -> false
        )
      )
    )

    crdlConnector
      .fetchCodeList("BC36")
      .map { _ shouldBe expected }
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
          "unitOfMeasureCode"                  -> "1",
          "degreePlatoApplicabilityFlag"       -> false,
          "actionIdentification"               -> "1103",
          "exciseProductsCategoryCode"         -> "E",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag"           -> false
        )
      ),
      CodeListEntry(
        "T300",
        "Cigars & cigarillos",
        Json.obj(
          "unitOfMeasureCode"                  -> "4",
          "degreePlatoApplicabilityFlag"       -> false,
          "actionIdentification"               -> "1116",
          "exciseProductsCategoryCode"         -> "T",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag"           -> false
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
          "unitOfMeasureCode"                  -> "3",
          "degreePlatoApplicabilityFlag"       -> false,
          "actionIdentification"               -> "1110",
          "exciseProductsCategoryCode"         -> "S",
          "alcoholicStrengthApplicabilityFlag" -> true,
          "densityApplicabilityFlag"           -> false
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
    val failedState   = "Failed"

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
    }
  }

  it should "retry when crdl-cache returns a server error" in {
    val retryScenario = "Retry"
    val failedState   = "Failed"

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
          "unitOfMeasureCode"                  -> "1",
          "degreePlatoApplicabilityFlag"       -> false,
          "actionIdentification"               -> "1103",
          "exciseProductsCategoryCode"         -> "E",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag"           -> false
        )
      ),
      CodeListEntry(
        "S200",
        "Spirituous beverages",
        Json.obj(
          "unitOfMeasureCode"                  -> "3",
          "degreePlatoApplicabilityFlag"       -> false,
          "actionIdentification"               -> "1110",
          "exciseProductsCategoryCode"         -> "S",
          "alcoholicStrengthApplicabilityFlag" -> true,
          "densityApplicabilityFlag"           -> false
        )
      ),
      CodeListEntry(
        "T300",
        "Cigars & cigarillos",
        Json.obj(
          "unitOfMeasureCode"                  -> "4",
          "degreePlatoApplicabilityFlag"       -> false,
          "actionIdentification"               -> "1116",
          "exciseProductsCategoryCode"         -> "T",
          "alcoholicStrengthApplicabilityFlag" -> false,
          "densityApplicabilityFlag"           -> false
        )
      )
    )

    crdlConnector
      .fetchCodeList("BC36")
      .map { _ shouldBe expected }
  }
}
