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

package uk.gov.hmrc.crdlcacheadminfrontend.codeLists.controllers

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.MimeTypes
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.*
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.crdlcacheadminfrontend.auth.Permissions
import uk.gov.hmrc.crdlcacheadminfrontend.codeLists.models.CodeListSnapshot
import uk.gov.hmrc.crdlcacheadminfrontend.codeLists.views.html.{ListDetail, Lists}
import uk.gov.hmrc.crdlcacheadminfrontend.connectors.CRDLConnector
import uk.gov.hmrc.crdlcacheadminfrontend.views.html.NotFound
import uk.gov.hmrc.crdlcacheadminfrontend.config.AppConfig
import uk.gov.hmrc.crdlcacheadminfrontend.models.paging.PagedResult
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client.Retrieval
import uk.gov.hmrc.internalauth.client.Retrieval.EmptyRetrieval
import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}

import java.util.UUID
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class CodeListsControllerSpec
  extends AnyFlatSpec
  with Matchers
  with MockitoSugar
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach {

  given ec: ExecutionContext              = ExecutionContext.global
  given mcc: MessagesControllerComponents = stubMessagesControllerComponents()

  private val authToken = UUID.randomUUID().toString

  private val configMock      = mock[AppConfig]
  private val authStub        = mock[StubBehaviour]
  private val crdlConnector   = mock[CRDLConnector]
  private val notFoundPage    = mock[NotFound]
  private val listsPage       = mock[Lists]
  private val listDetailsPage = mock[ListDetail]

  private val defaultPageNum     = 1
  private val defaultPageSize    = 10
  private val defaultItemsInPage = 10
  private val defaultTotalItems  = 10
  private val defaultTotalPages  = 1

  val controller = new CodeListsController(
    configMock,
    FrontendAuthComponentsStub(authStub),
    crdlConnector,
    mcc,
    notFoundPage,
    listsPage,
    listDetailsPage
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(authStub, crdlConnector, notFoundPage, listsPage, listDetailsPage)
  }

  val timestamp = Instant.now()

  val BC36  = CodeListSnapshot("BC36", 21, None, None, Some(timestamp))
  val BC108 = CodeListSnapshot("BC108", 7, None, None, Some(timestamp))
  val BC08  = CodeListSnapshot("BC08", 17, None, None, Some(timestamp))
  val BC66  = CodeListSnapshot("BC66", 10, None, None, Some(timestamp))
  val CL190 = CodeListSnapshot("CL190", 2, Some("P6"), Some("NCTS"), Some(timestamp))

  val codeListSnapshots = List(BC36, BC108, BC08, BC66, CL190)

  "CodeListsController.viewLists" should "return 200 OK when there are no errors" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.unit)

    // TODO: Decide what to do about lexical ordering as it will sort 1xx codes awkwardly
    val orderedSnapshots = List(BC08, BC108, BC36, BC66, CL190)
    val pagedResult = PagedResult(
      orderedSnapshots,
      defaultPageNum,
      defaultPageSize,
      defaultItemsInPage,
      defaultTotalItems,
      defaultTotalPages
    )

    when(
      crdlConnector.fetchCodeListSnapShots(
        eqTo(defaultPageNum),
        eqTo(defaultPageSize),
        eqTo(None),
        eqTo(None),
        eqTo(None)
      )(using
        any(),
        eqTo(ec)
      )
    ).thenReturn(Future.successful(pagedResult))

    when(
      listsPage(eqTo(pagedResult), eqTo(None), eqTo(None), eqTo(None))(using any(), any())
    ).thenReturn(HtmlFormat.empty)

    val request = FakeRequest().withSession("authToken" -> authToken)
    val result =
      controller.viewLists(Some(defaultPageNum), Some(defaultPageSize), None, None, None)(request)

    status(result) shouldBe OK
    contentType(result) shouldBe Some(MimeTypes.HTML)
  }

  it should "pass codeListCode filter to the connector and view" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.unit)

    val pagedResult = PagedResult(List(BC08), defaultPageNum, defaultPageSize, 1, 1, 1)

    when(
      crdlConnector.fetchCodeListSnapShots(
        eqTo(defaultPageNum),
        eqTo(defaultPageSize),
        eqTo(Some("BC08")),
        eqTo(None),
        eqTo(None)
      )(using
        any(),
        eqTo(ec)
      )
    ).thenReturn(Future.successful(pagedResult))

    when(
      listsPage(eqTo(pagedResult), eqTo(Some("BC08")), eqTo(None), eqTo(None))(using any(), any())
    ).thenReturn(HtmlFormat.empty)

    val request = FakeRequest().withSession("authToken" -> authToken)
    val result =
      controller.viewLists(Some(defaultPageNum), Some(defaultPageSize), Some("BC08"), None, None)(
        request
      )

    status(result) shouldBe OK
    contentType(result) shouldBe Some(MimeTypes.HTML)
  }

  it should "pass phase and domain filters to the connector and view" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.unit)

    val pagedResult = PagedResult(List(CL190), defaultPageNum, defaultPageSize, 1, 1, 1)

    when(
      crdlConnector.fetchCodeListSnapShots(
        eqTo(defaultPageNum),
        eqTo(defaultPageSize),
        eqTo(None),
        eqTo(Some("P6")),
        eqTo(Some("NCTS"))
      )(using
        any(),
        eqTo(ec)
      )
    ).thenReturn(Future.successful(pagedResult))

    when(
      listsPage(eqTo(pagedResult), eqTo(None), eqTo(Some("P6")), eqTo(Some("NCTS")))(using
        any(),
        any()
      )
    ).thenReturn(HtmlFormat.empty)

    val request = FakeRequest().withSession("authToken" -> authToken)
    val result = controller.viewLists(
      Some(defaultPageNum),
      Some(defaultPageSize),
      None,
      Some("P6"),
      Some("NCTS")
    )(request)

    status(result) shouldBe OK
    contentType(result) shouldBe Some(MimeTypes.HTML)
  }

  it should "treat empty string filter values as absent (None)" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.unit)

    val pagedResult = PagedResult(List(BC08), defaultPageNum, defaultPageSize, 1, 1, 1)

    when(
      crdlConnector.fetchCodeListSnapShots(
        eqTo(defaultPageNum),
        eqTo(defaultPageSize),
        eqTo(None),
        eqTo(None),
        eqTo(None)
      )(using
        any(),
        eqTo(ec)
      )
    ).thenReturn(Future.successful(pagedResult))

    when(
      listsPage(eqTo(pagedResult), eqTo(None), eqTo(None), eqTo(None))(using any(), any())
    ).thenReturn(HtmlFormat.empty)

    val request = FakeRequest().withSession("authToken" -> authToken)
    val result = controller.viewLists(
      Some(defaultPageNum),
      Some(defaultPageSize),
      Some(""),
      Some(""),
      Some("")
    )(request)

    status(result) shouldBe OK
  }

  it should "throw errors returned when calling internal-auth to the top level error handler" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.failed(UpstreamErrorResponse("Oh no!", INTERNAL_SERVER_ERROR)))

    val request = FakeRequest().withSession("authToken" -> authToken)

    assertThrows[UpstreamErrorResponse] {
      await(
        controller.viewLists(Some(defaultPageNum), Some(defaultPageSize), None, None, None)(request)
      )
    }
  }

  it should "throw errors returned when calling crdl-cache to the top level error handler" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.unit)

    when(
      crdlConnector.fetchCodeListSnapShots(
        eqTo(defaultPageNum),
        eqTo(defaultPageSize),
        eqTo(None),
        eqTo(None),
        eqTo(None)
      )(using
        any(),
        eqTo(ec)
      )
    )
      .thenReturn(Future.failed(UpstreamErrorResponse("Oh no!", INTERNAL_SERVER_ERROR)))

    val request = FakeRequest().withSession("authToken" -> authToken)

    assertThrows[UpstreamErrorResponse] {
      await(
        controller.viewLists(Some(defaultPageNum), Some(defaultPageSize), None, None, None)(request)
      )
    }
  }

  it should "redirect the user to login when they have no active session or an invalid auth token" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

    val request = FakeRequest().withSession("authToken" -> authToken)
    val result =
      controller.viewLists(Some(defaultPageNum), Some(defaultPageSize), None, None, None)(request)

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(
      "/internal-auth-frontend/sign-in?continue_url=%2Fcrdl-cache-admin-frontend%2Flists"
    )
  }

  // TODO: This test characterises the current behaviour, but we should supply a "Not authorized" page that tells the user they aren't authorised to use this frontend
  it should "throw an error to the top level error handler when the user's auth token does not provide the appropriate permissions" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.failed(UpstreamErrorResponse("Forbidden", FORBIDDEN)))

    val request = FakeRequest().withSession("authToken" -> authToken)

    assertThrows[UpstreamErrorResponse] {
      await(
        controller.viewLists(Some(defaultPageNum), Some(defaultPageSize), None, None, None)(request)
      )
    }
  }
}
