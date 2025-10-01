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

import javax.inject.{Inject, Singleton}
import play.api.mvc.MessagesControllerComponents
import scala.concurrent.Future.successful
import uk.gov.hmrc.crdlcacheadminfrontend.auth.AuthActions
import uk.gov.hmrc.crdlcacheadminfrontend.connectors.CRDLConnector
import uk.gov.hmrc.crdlcacheadminfrontend.codeLists.views.html.{Lists, ListDetail}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents

@Singleton
class CodeListsController @Inject(
    auth: FrontendAuthComponents, 
    authActions: AuthActions,
    crdlConnector: CRDLConnector,
    mcc: MessagesControllerComponents,
    listsPage: Lists,
    listDetailsPage: ListDetail
)(using ExecutionContext) extends FrontendController(mcc) {
    given HeaderCarrier = HeaderCarrier()
    given FrontendAuthComponents = auth
    
    def viewLists = Action.async { implicit request => {
        authActions.handle(
            routes.CodeListsController.viewLists(),
            r => {
                val snapshots = Await.result(crdlConnector.fetchCodeListSnapShots(), Duration.Inf)
                successful(Ok(listsPage(snapshots.sortWith((a, b) => a.codeListCode.toLowerCase() < b.codeListCode.toLowerCase()))))
            }
        )
    }}

    def listDetail(code: String) = Action.async { implicit request => {
        authActions.handle(
            routes.CodeListsController.listDetail(code),
            r => {
                val snapshot = Await.result(crdlConnector.fetchCodeListSnapShots(), Duration.Inf)
                    .find(s => s.codeListCode == code).getOrElse(null)
                val codeLists = Await.result(crdlConnector.fetchCodeList(code), Duration.Inf)
                successful(Ok(listDetailsPage(code, snapshot, codeLists)))
            }
        )
    }}
}
