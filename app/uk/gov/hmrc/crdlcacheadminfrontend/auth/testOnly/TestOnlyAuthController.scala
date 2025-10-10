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

package uk.gov.hmrc.crdlcacheadminfrontend.auth.testOnly

import javax.inject.Inject
import play.api.mvc.MessagesControllerComponents
import scala.concurrent.Future.successful
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import java.net.URLEncoder
import java.nio.charset.Charset

/** This takes the internal auth frontend redirect url of /test-only/sign-in?continue_url=... as
  * this is served without a host out on cloud environments and redirects it the the local host for
  * this service after reconfiguring the continueUrl to return to the same path on this service's
  * host
  */
class TestOnlyAuthController @Inject() (mcc: MessagesControllerComponents)
  extends FrontendController(mcc) {

  def localRedirect() = Action.async { implicit request =>
    val continueUrl = request.getQueryString("continue_url").getOrElse("")
    successful(
      Redirect(
        s"http://localhost:8471/test-only/sign-in?continue_url=${URLEncoder.encode(s"http://${request.host}$continueUrl", "UTF-8")}"
      )
    )
  }

}
