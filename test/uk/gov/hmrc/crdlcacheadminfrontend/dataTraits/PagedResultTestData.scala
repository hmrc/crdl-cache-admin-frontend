/*
 * Copyright 2026 HM Revenue & Customs
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

import uk.gov.hmrc.crdlcacheadminfrontend.models.paging.PagedResult
import play.api.libs.json.{Json, Format}

trait PagedResultTestData {
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

  def asJson[T](pagedResult: PagedResult[T])(implicit formatT: Format[T]): String =
    Json.toJson(pagedResult).toString
}
