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

package uk.gov.hmrc.crdlcacheadminfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import scala.concurrent.Future.successful
import uk.gov.hmrc.crdlcacheadminfrontend.auth.AuthActions
import uk.gov.hmrc.crdlcacheadminfrontend.views.html.Index
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents


@Singleton
class IndexController @Inject()(
  auth: FrontendAuthComponents, 
  authActions: AuthActions,
  mcc: MessagesControllerComponents,
  indexPage: Index
) extends FrontendController(mcc) with I18nSupport {
    def onPageLoad() = 
      auth.authorizedAction(
          continueUrl = routes.IndexController.onPageLoad(),
          predicate = authActions.permission
      ).async { implicit request =>
          successful(Ok(indexPage()))
      }
}
