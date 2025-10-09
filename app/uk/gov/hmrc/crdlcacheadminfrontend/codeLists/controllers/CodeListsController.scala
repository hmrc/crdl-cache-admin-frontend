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
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.crdlcacheadminfrontend.auth.Permissions
import uk.gov.hmrc.crdlcacheadminfrontend.connectors.CRDLConnector
import uk.gov.hmrc.crdlcacheadminfrontend.views.html.NotFound
import uk.gov.hmrc.crdlcacheadminfrontend.codeLists.views.html.{Lists, ListDetail}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import play.api.i18n.Messages

@Singleton
class CodeListsController @Inject(
    auth: FrontendAuthComponents, 
    crdlConnector: CRDLConnector,
    mcc: MessagesControllerComponents,
    notFoundPage: NotFound,
    listsPage: Lists,
    listDetailsPage: ListDetail
)(using ExecutionContext) extends FrontendController(mcc) with I18nSupport {    
    def viewLists =
        auth.authorizedAction(
          continueUrl = routes.CodeListsController.viewLists(),
          predicate = Permissions.read
        ).async { implicit request =>
            crdlConnector.fetchCodeListSnapShots().map { snapshots =>
                val sortedSnapshots = snapshots.sortWith((a, b) => a.codeListCode.toLowerCase() < b.codeListCode.toLowerCase())
                Ok(listsPage(sortedSnapshots))
            }
        }

    def listDetail(code: String) =
        auth.authorizedAction(
          continueUrl = routes.CodeListsController.listDetail(code),
          predicate = Permissions.read
        ).async { implicit request => 
                val codeListsFuture =  crdlConnector.fetchCodeList(code)
                val snapshotsFuture =  crdlConnector.fetchCodeListSnapShots()
                for {
                    codeLists <- codeListsFuture
                    snapshots <- snapshotsFuture
                } yield {
                    snapshots.find(s => s.codeListCode == code).fold {
                        val messages = summon[Messages]
                        Ok(notFoundPage(
                            messages("error.codelist.snapshot.notfound.heading", code),
                            messages("error.codelist.snapshot.notfound.text", code)
                        ))
                    } { snapshot =>
                        Ok(listDetailsPage(code, snapshot, codeLists))
                    }
                }
        }
}
