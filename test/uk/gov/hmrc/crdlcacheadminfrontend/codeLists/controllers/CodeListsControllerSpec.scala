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

  private val authStub        = mock[StubBehaviour]
  private val crdlConnector   = mock[CRDLConnector]
  private val notFoundPage    = mock[NotFound]
  private val listsPage       = mock[Lists]
  private val listDetailsPage = mock[ListDetail]

  val controller = new CodeListsController(
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

  val BC36  = CodeListSnapshot("BC36", 21, Some(timestamp))
  val BC108 = CodeListSnapshot("BC108", 7, Some(timestamp))
  val BC08  = CodeListSnapshot("BC08", 17, Some(timestamp))
  val BC66  = CodeListSnapshot("BC66", 10, Some(timestamp))

  val codeListSnapshots = List(BC36, BC108, BC08, BC66)

  "CodeListsController.viewLists" should "return 200 OK when there are no errors" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.unit)

    when(crdlConnector.fetchCodeListSnapShots()(using any(), eqTo(ec)))
      .thenReturn(Future.successful(codeListSnapshots))

    // TODO: Decide what to do about lexical ordering as it will sort 1xx codes awkwardly
    val orderedSnapshots = List(BC08, BC108, BC36, BC66)

    when(listsPage(eqTo(orderedSnapshots))(using any(), any())).thenReturn(HtmlFormat.empty)

    val request = FakeRequest().withSession("authToken" -> authToken)
    val result  = controller.viewLists(request)

    status(result) shouldBe OK
    contentType(result) shouldBe Some(MimeTypes.HTML)
  }

  it should "throw errors returned when calling internal-auth to the top level error handler" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.failed(UpstreamErrorResponse("Oh no!", INTERNAL_SERVER_ERROR)))

    when(crdlConnector.fetchCodeListSnapShots()(using any(), eqTo(ec)))
      .thenReturn(Future.successful(List.empty))

    val request = FakeRequest().withSession("authToken" -> authToken)

    assertThrows[UpstreamErrorResponse] {
      await(controller.viewLists(request))
    }
  }

  it should "throw errors returned when calling crdl-cache to the top level error handler" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.unit)

    when(crdlConnector.fetchCodeListSnapShots()(using any(), eqTo(ec)))
      .thenReturn(Future.failed(UpstreamErrorResponse("Oh no!", INTERNAL_SERVER_ERROR)))

    val request = FakeRequest().withSession("authToken" -> authToken)

    assertThrows[UpstreamErrorResponse] {
      await(controller.viewLists(request))
    }
  }

  it should "redirect the user to login when they have no active session or an invalid auth token" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

    when(crdlConnector.fetchCodeListSnapShots()(using any(), eqTo(ec)))
      .thenReturn(Future.successful(List.empty))

    val request = FakeRequest().withSession("authToken" -> authToken)
    val result  = controller.viewLists(request)

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(
      "/internal-auth-frontend/sign-in?continue_url=%2Fcrdl-cache-admin-frontend%2Flists"
    )
  }

  // TODO: This test characterises the current behaviour, but we should supply a "Not authorized" page that tells the user they aren't authorised to use this frontend
  it should "throw an error to the top level error handler when the user's auth token does not provide the appropriate permissions" in {
    when(authStub.stubAuth(predicate = Some(Permissions.read), retrieval = EmptyRetrieval))
      .thenReturn(Future.failed(UpstreamErrorResponse("Forbidden", FORBIDDEN)))

    when(crdlConnector.fetchCodeListSnapShots()(using any(), eqTo(ec)))
      .thenReturn(Future.successful(List.empty))

    val request = FakeRequest().withSession("authToken" -> authToken)

    assertThrows[UpstreamErrorResponse] {
      await(controller.viewLists(request))
    }
  }
}
