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

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.crdlcacheadminfrontend.auth.AuthActions
import uk.gov.hmrc.crdlcacheadminfrontend.connectors.CRDLConnector
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.views.html.{Offices, OfficeDetails}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.models.CustomsOffice

@Singleton
class CustomsOfficesController @Inject(
    auth: FrontendAuthComponents, 
    authActions: AuthActions,
    crdlConnector: CRDLConnector,
    mcc: MessagesControllerComponents,
    officesPage: Offices,
    officeDetailsPage: OfficeDetails
)(using ExecutionContext) extends FrontendController(mcc) with I18nSupport {    
    def viewOffices =
        auth.authorizedAction(
            continueUrl = routes.CustomsOfficesController.viewOffices(),
            predicate = authActions.permission
        ).async { implicit request =>
            crdlConnector.fetchCustomsOffices().map(customsOffices =>
                val sortedOffices = customsOffices.sortWith((a, b) => a.referenceNumber.toLowerCase() < b.referenceNumber.toLowerCase())
                Ok(officesPage(sortedOffices))
            )
        }

    def officeDetail(referenceNumber: String) =
        auth.authorizedAction(
            continueUrl = routes.CustomsOfficesController.viewOffices(),
            predicate = authActions.permission
        ).async { implicit request =>
            crdlConnector.fetchCustomsOffices(referenceNumbers = Some(Set(referenceNumber))).map(customsOffices =>
                Ok(officeDetailsPage(CustomsOffice.toViewModel(customsOffices(0))))
            )
        }
}
