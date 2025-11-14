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

package uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.controllers

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatest.BeforeAndAfterEach
import scala.concurrent.ExecutionContext
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.*
import uk.gov.hmrc.crdlcacheadminfrontend.config.AppConfig
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.crdlcacheadminfrontend.connectors.CRDLConnector
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.views.html.{Offices, OfficeDetails}
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import uk.gov.hmrc.crdlcacheadminfrontend.auth.Permissions
import uk.gov.hmrc.internalauth.client.Retrieval.EmptyRetrieval
import scala.concurrent.Future
import play.twirl.api.HtmlFormat
import java.util.UUID
import play.api.test.FakeRequest
import play.api.http.MimeTypes
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.crdlcacheadminfrontend.models.paging.PagedResult
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.models.CustomsOfficeSummary
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.models.CustomsOffice
import uk.gov.hmrc.crdlcacheadminfrontend.dataTraits.CustomsOfficeSummaryTestData
import uk.gov.hmrc.crdlcacheadminfrontend.dataTraits.CustomsOfficeTestData

class CustomsOfficesControllerSpec
  extends AnyFlatSpec
  with Matchers
  with MockitoSugar
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach
  with CustomsOfficeSummaryTestData
  with CustomsOfficeTestData {
  private val defaultPageNum = 1
  private val defaultPageSize = 10
  private val defaultReferenceNumber = "TestRef"
  private val defaultReferenceNumberSet = Some(Set(defaultReferenceNumber))
  private val defaultAuthToken = UUID.randomUUID().toString
  private val defaultRequest = FakeRequest().withSession("authToken" -> defaultAuthToken)
  private val expectedOfficesRedirectUrl = "/internal-auth-frontend/sign-in?continue_url=%2Fcrdl-cache-admin-frontend%2F"

  given ec: ExecutionContext              = ExecutionContext.global
  given mcc: MessagesControllerComponents = stubMessagesControllerComponents()

  private val configMock            = mock[AppConfig]
  private val authStub              = mock[StubBehaviour]
  private val crdlConnectorMock     = mock[CRDLConnector]
  private val officesPageMock       = mock[Offices]
  private val officeDetailsPageMock = mock[OfficeDetails]

  val controller = new CustomsOfficesController(
    configMock,
    FrontendAuthComponentsStub(authStub),
    crdlConnectorMock,
    mcc,
    officesPageMock,
    officeDetailsPageMock
  )

  // Auth Stub behaviour
  def stubAuth_Successful() =
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
    .thenReturn(Future.unit)

  def stubAuth_ThrowsUpstreamErrorResponse(responseErrorCode: Int = INTERNAL_SERVER_ERROR) =
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.failed(UpstreamErrorResponse("Auth error", responseErrorCode)))

  // CRDLConnector Stub Behaviour...
  // ...fetchCustomsOfficeSummaries
  def stubCRDLConnector_FetchCustomsOfficeSummaries_Successful (
    payload: PagedResult[CustomsOfficeSummary] = pagedCustomsOfficeSummaryResult,
    pageNum: Int = defaultPageNum,
    pageSize: Int = defaultPageSize
  ) =
    when(crdlConnectorMock.fetchCustomsOfficeSummaries(eqTo(pageNum), eqTo(pageSize))(using any(), eqTo(ec)))
      .thenReturn(Future.successful(payload))

  def stubCRDLConnector_FetchCustomsOfficeSummaries_ThrowsUpsteamErrorResponse (
    pageNum: Int = defaultPageNum,
    pageSize: Int = defaultPageSize
  ) =
    when(crdlConnectorMock.fetchCustomsOfficeSummaries(eqTo(pageNum), eqTo(pageSize))(using any(), eqTo(ec)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Customs office summaries called failed", INTERNAL_SERVER_ERROR)))

  // ...fetchCustomsOffices
  def stubCRDLConnector_FetchCustomsOffices_Successful(
    payload: List[CustomsOffice] = defaultCustomsOfficeList,
    referenceNumbers: Option[Set[String]] = defaultReferenceNumberSet
  ) =
    when(crdlConnectorMock.fetchCustomsOffices(eqTo(referenceNumbers), any(), any(), any())(using any(), eqTo(ec)))
      .thenReturn(Future.successful(payload))

  def stubCRDLConnector_FetchCustomsOffices_ThrowsUpsteamErrorResponse (
    referenceNumbers: Option[Set[String]] = defaultReferenceNumberSet
  ) =
    when(crdlConnectorMock.fetchCustomsOffices(eqTo(referenceNumbers), any(), any(), any())(using any(), eqTo(ec)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Customs office summaries called failed", INTERNAL_SERVER_ERROR)))

  // Pages stubs behaviour
  def stubOfficesPage_Successful() =
    when(officesPageMock(eqTo(pagedCustomsOfficeSummaryResult))(using any(), any()))
      .thenReturn(HtmlFormat.empty)
  def stubOfficeDetailPage_Successful() =
    when(officeDetailsPageMock(any())(using any(), any()))
      .thenReturn(HtmlFormat.empty)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(configMock, authStub, crdlConnectorMock, officesPageMock, officeDetailsPageMock)
  }
  
  "CustomsOfficesController.viewOffices" should "return 200 OK when there are no errors" in {
    stubAuth_Successful()
    stubCRDLConnector_FetchCustomsOfficeSummaries_Successful()
    stubOfficesPage_Successful()

    val result = controller.viewOffices(Some(defaultPageNum), Some(defaultPageSize))(defaultRequest)

    status(result) shouldBe OK
    contentType(result) shouldBe Some(MimeTypes.HTML)
  }

  it should "throws the returned error when internal-auth fails" in {
    stubAuth_ThrowsUpstreamErrorResponse()
    stubCRDLConnector_FetchCustomsOfficeSummaries_Successful()
    stubOfficesPage_Successful()

    assertThrows[UpstreamErrorResponse] {
      await(controller.viewOffices(Some(defaultPageNum), Some(defaultPageSize))(defaultRequest))
    }
  }

  // TODO: This test characterises the current behaviour, but we should supply a "Not authorized" page that tells the user they aren't authorised to use this frontend
  it should "throws the returned error when internal-auth determines that the user does not the required permissions" in {
    stubAuth_ThrowsUpstreamErrorResponse(FORBIDDEN)
    stubCRDLConnector_FetchCustomsOfficeSummaries_Successful()
    stubOfficesPage_Successful()

    assertThrows[UpstreamErrorResponse] {
      await(controller.viewOffices(Some(defaultPageNum), Some(defaultPageSize))(defaultRequest))
    }
  }

  it should "redirect the user to login when they are unauthorized" in {
    stubAuth_ThrowsUpstreamErrorResponse(UNAUTHORIZED)
    stubCRDLConnector_FetchCustomsOfficeSummaries_Successful()
    stubOfficesPage_Successful()

    val result = controller.viewOffices(Some(defaultPageNum), Some(defaultPageSize))(defaultRequest)

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(
      s"${expectedOfficesRedirectUrl}offices"
    )
  }

  it should "throws the returned error when the crdl cache call fails" in {
    stubAuth_Successful()
    stubCRDLConnector_FetchCustomsOfficeSummaries_ThrowsUpsteamErrorResponse()
    stubOfficesPage_Successful()

    assertThrows[UpstreamErrorResponse] {
      await(controller.viewOffices(Some(defaultPageNum), Some(defaultPageSize))(defaultRequest))
    }
  }

  "CustomsOfficesController.officeDetail" should "return 200 OK when there are no errors" in {
    stubAuth_Successful()
    stubCRDLConnector_FetchCustomsOffices_Successful()
    stubOfficeDetailPage_Successful()

    val result = controller.officeDetail(defaultReferenceNumber)(defaultRequest)

    status(result) shouldBe OK
    contentType(result) shouldBe Some(MimeTypes.HTML)
  }

  it should "throws the returned error when internal-auth fails" in {
    stubAuth_ThrowsUpstreamErrorResponse()
    stubCRDLConnector_FetchCustomsOffices_Successful()
    stubOfficeDetailPage_Successful()

    assertThrows[UpstreamErrorResponse] {
      await(controller.officeDetail(defaultReferenceNumber)(defaultRequest))
    }
  }

  // TODO: This test characterises the current behaviour, but we should supply a "Not authorized" page that tells the user they aren't authorised to use this frontend
  it should "throws the returned error when internal-auth determines that the user does not the required permissions" in {
    stubAuth_ThrowsUpstreamErrorResponse(FORBIDDEN)
    stubCRDLConnector_FetchCustomsOffices_Successful()
    stubOfficeDetailPage_Successful()

    assertThrows[UpstreamErrorResponse] {
      await(controller.officeDetail(defaultReferenceNumber)(defaultRequest))
    }
  }

  it should "redirect the user to login when they are unauthorized" in {
    stubAuth_ThrowsUpstreamErrorResponse(UNAUTHORIZED)
    stubCRDLConnector_FetchCustomsOffices_Successful()
    stubOfficeDetailPage_Successful()

    val result = controller.officeDetail(defaultReferenceNumber)(defaultRequest)

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(
      s"${expectedOfficesRedirectUrl}offices"
    )
  }

  it should "throws the returned error when the crdl cache call fails" in {
    stubAuth_Successful()
    stubCRDLConnector_FetchCustomsOffices_ThrowsUpsteamErrorResponse()
    stubOfficeDetailPage_Successful()

    assertThrows[UpstreamErrorResponse] {
      await(controller.officeDetail(defaultReferenceNumber)(defaultRequest))
    }
  }
}
