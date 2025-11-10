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
import uk.gov.hmrc.crdlcacheadminfrontend.dataTraits.CustomsOfficeSummaryTestData

class CRDLConnectorSpec
  extends AsyncFlatSpec
  with Matchers
  with WireMockSupport
  with HttpClientV2Support
  with CustomsOfficeSummaryTestData {
    given actorSystem: ActorSystem = ActorSystem("test")
    given HeaderCarrier = HeaderCarrier()

    private val officeSumamriesUrl = "/crdl-cache/v2/offices/summaries"

    private val appConfig = new AppConfig(
        Configuration(
            "microservice.services.crdl-cache.protocol" -> "http",
            "microservice.services.crdl-cache.host"     -> "localhost",
            "microservice.services.crdl-cache.port"     -> wireMockPort,
            "http-verbs.retries.intervals"              -> List("1.millis")
        )
    )
  
    private val connector = new CRDLConnector(
        appConfig, httpClientV2
    )

    private val retryScenario = "Retry"
    private val failedState   = "Failed"

    def customsOfficeSummariesShouldError(errorResponse:() => ResponseDefinitionBuilder) = {
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

    def customsOfficeSummariesTestRetry(errorResponse:() => ResponseDefinitionBuilder, shouldRetry: Boolean) = {
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

        if(shouldRetry) {
            connector.fetchCustomsOfficeSummaries(1, 10)
                .map(_ mustBe pagedCustomsOfficeSummaryResult)
        }
        else {
            recoverToSucceededIf[UpstreamErrorResponse] {
                connector.fetchCustomsOfficeSummaries(1, 10)
            }
        }
    }

    "CRDLConnector.fetchCustomsOfficeSummaries: return the data as delivered from the API" should "return the data as delivered from the API" in {
        val expectedResult = pagedCustomsOfficeSummaryResult
        val pageNum = 1
        val pageSize = pagedCustomsOfficeSummaryResult.items.length

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
            .map(_ mustBe expectedResult )
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
}
