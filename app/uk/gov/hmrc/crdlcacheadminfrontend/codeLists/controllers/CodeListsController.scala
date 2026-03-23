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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.crdlcacheadminfrontend.auth.Permissions
import uk.gov.hmrc.crdlcacheadminfrontend.codeLists.models.CodeListEntry
import uk.gov.hmrc.crdlcacheadminfrontend.connectors.CRDLConnector
import uk.gov.hmrc.crdlcacheadminfrontend.views.html.NotFound
import uk.gov.hmrc.crdlcacheadminfrontend.codeLists.views.html.{ListDetail, Lists}
import uk.gov.hmrc.crdlcacheadminfrontend.config.AppConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents

@Singleton
class CodeListsController @Inject (
  config: AppConfig,
  auth: FrontendAuthComponents,
  crdlConnector: CRDLConnector,
  mcc: MessagesControllerComponents,
  notFoundPage: NotFound,
  listsPage: Lists,
  listDetailsPage: ListDetail
)(using ExecutionContext)
  extends FrontendController(mcc)
  with I18nSupport {

  def viewLists(
    page: Option[Int],
    pageSize: Option[Int],
    codeListCode: Option[String],
    phase: Option[String],
    domain: Option[String]
  ): Action[AnyContent] =
    auth
      .authorizedAction(
        continueUrl = routes.CodeListsController.viewLists(),
        predicate = Permissions.read
      )
      .async { implicit request =>
        crdlConnector
          .fetchCodeListSnapShots(
            page.getOrElse(1),
            pageSize.getOrElse(config.defaultPageSize),
            codeListCode.filter(_.nonEmpty),
            phase.filter(_.nonEmpty),
            domain.filter(_.nonEmpty)
          )
          .map { snapshots =>
            Ok(
              listsPage(
                snapshots,
                codeListCode.filter(_.nonEmpty),
                phase.filter(_.nonEmpty),
                domain.filter(_.nonEmpty)
              )
            )
          }
      }

  def listDetail(code: String): Action[AnyContent] =
    auth
      .authorizedAction(
        continueUrl = routes.CodeListsController.listDetail(code),
        predicate = Permissions.read
      )
      .async { implicit request =>
        val messages = request.messages
        for {
          snapshots <- crdlConnector.fetchCodeListSnapShot(code)
          codeLists <- snapshots.fold(Future.successful(List.empty[CodeListEntry]))(s =>
            crdlConnector.fetchCodeList(code, phase = s.phase, domain = s.domain)
          )
        } yield {
          snapshots.fold {
            Ok(
              notFoundPage(
                messages("error.codelist.snapshot.notfound.heading", code),
                messages("error.codelist.snapshot.notfound.text", code)
              )
            )
          } { snapshot =>
            Ok(listDetailsPage(code, snapshot, codeLists))
          }
        }
      }
}
