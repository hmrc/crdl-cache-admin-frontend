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
import play.api.mvc.MessagesControllerComponents
import scala.concurrent.Future.successful
import uk.gov.hmrc.crdlcacheadminfrontend.auth.AuthActions
import uk.gov.hmrc.crdlcacheadminfrontend.connectors.CRDLConnector
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.models.CustomsOffice
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.views.html.{Offices, OfficeDetails}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents

@Singleton
class CustomsOfficesController @Inject(
    auth: FrontendAuthComponents, 
    authActions: AuthActions,
    crdlConnector: CRDLConnector,
    mcc: MessagesControllerComponents,
    officesPage: Offices,
    officeDetailsPage: OfficeDetails
)(using ExecutionContext) extends FrontendController(mcc) {
    given HeaderCarrier = HeaderCarrier()
    given FrontendAuthComponents = auth
    
    def viewOffices = Action.async { implicit request => {
        authActions.handle(
            routes.CustomsOfficesController.viewOffices(),
            r => {
                val customOffices = Await.result(crdlConnector.fetchCustomsOffices(), Duration.Inf)
                successful(Ok(officesPage(customOffices.sortWith((a, b) => a.referenceNumber.toLowerCase() < b.referenceNumber.toLowerCase()))))
            }
        )
    }}

    def officeDetail(referenceNumber: String) = Action.async { implicit request => {
        authActions.handle(
            routes.CustomsOfficesController.officeDetail(referenceNumber),
            r => {
                val customsOffice = Await.result(crdlConnector.fetchCustomsOffices(referenceNumbers = Some(Set(referenceNumber))), Duration.Inf)
                successful(Ok(officeDetailsPage(customsOffice(0))))
            }
        )
    }}
}
