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

package uk.gov.hmrc.crdlcacheadminfrontend.auth

import play.api.mvc.MessagesRequest
import play.api.mvc.AnyContent
import uk.gov.hmrc.internalauth.client.Predicate
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import uk.gov.hmrc.internalauth.client.{AuthenticatedRequest, IAAction, Resource, ResourceLocation, ResourceType}
import scala.concurrent.Future
import play.api.mvc.{Call, Result}

class AuthActions {
    val permission = Predicate.Permission(
        Resource(
          ResourceType("crdl-cache"),
          ResourceLocation("*")
        ),
        IAAction("READ"))
        
    def handle(
        returnCall: Call,
        block: AuthenticatedRequest[AnyContent, Unit] => Future[Result]
    )(implicit auth: FrontendAuthComponents, request : MessagesRequest[AnyContent]) = 
        auth.authorizedAction(
            continueUrl = returnCall,
            predicate   = permission
        ).async{implicit request => {
            block(request)
        }}(request)
}