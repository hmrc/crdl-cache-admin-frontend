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

import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.models.CustomsOfficeSummary
import uk.gov.hmrc.crdlcacheadminfrontend.models.paging.PagedResult
import play.api.libs.json.{Json, Writes}

case class CustomsOfficeSummaryOptionss()

trait CustomsOfficeSummaryTestData {
  def generateCustomOfficeSummary(key: String) = CustomsOfficeSummary(
        referenceNumber = s"$key-ReferenceNumber",
        countryCode = "CC",
        customsOfficeUsualName = s"$key Customs Office"
  )
  def pagedResult[T](
    items: Seq[T],
    pageNum: Option[Int] = None,
    pageSize: Option[Int] = None,
    itemsInPage: Option[Int] = None,
    totalItems: Option[Int] = None,
    totalPages: Option[Int] = None
  ): PagedResult[T] = PagedResult(
    items = items,
    pageNum = pageNum.getOrElse(1),
    pageSize = pageSize.getOrElse(items.length),
    itemsInPage = itemsInPage.getOrElse(items.length),
    totalItems = totalItems.getOrElse(items.length),
    totalPages = totalPages.getOrElse(1)
  )

  def asJson[T](pagedResult: PagedResult[T])(implicit writesT: Writes[T]): String =
    Json.toJson(pagedResult).toString

  lazy val pagedCustomsOfficeSummaryResult: PagedResult[CustomsOfficeSummary] =
      pagedResult(
        Seq(
          generateCustomOfficeSummary("01"),
          generateCustomOfficeSummary("02"),
          generateCustomOfficeSummary("03"),
          generateCustomOfficeSummary("04"),
          generateCustomOfficeSummary("05"),
        ),
        totalItems = Some(15),
        totalPages = Some(3)
      )
}
