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

package uk.gov.hmrc.crdlcacheadminfrontend.dataTraits

import uk.gov.hmrc.crdlcacheadminfrontend.codeLists.models.CodeListSnapshot
import uk.gov.hmrc.crdlcacheadminfrontend.models.paging.PagedResult
import java.time.Instant

case class CodeListSnapShotsOptions()

trait CodeListSnapShotsTestData extends PagedResultTestData {
  def generateCodeList(key: String, version: Int) = CodeListSnapshot(
    codeListCode = s"CL$key",
    snapshotVersion = version,
    phase = Some("P6"),
    domain = Some("NCTS"),
    lastUpdated = Some(Instant.now())
  )

  lazy val pagedCodeListSnapShotResult: PagedResult[CodeListSnapshot] =
    pagedResult(
      Seq(
        generateCodeList("001", 1),
        generateCodeList("002", 2),
        generateCodeList("003", 3),
        generateCodeList("004", 4),
        generateCodeList("005", 5)
      ),
      totalItems = Some(15),
      totalPages = Some(3)
    )
}
