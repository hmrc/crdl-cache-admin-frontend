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

package uk.gov.hmrc.crdlcacheadminfrontend.utils

import java.time.format.DateTimeFormatter
import java.time.Instant
import play.api.i18n.Messages
import java.time.ZoneId
import java.time.LocalDate
import java.time.LocalTime

object ViewUtils {
  def pageTitle(title: String)(implicit messages: Messages) =
    messages("page.title", title)

  val baseUrl = "crdl-cache-admin-frontend"

  def withBaseUrl(url: String): String = s"/$baseUrl/$url"

  val listsUrl                    = withBaseUrl("lists")
  def listDetailUrl(code: String) = withBaseUrl(s"lists/$code")

  val officesUrl                               = withBaseUrl("offices")
  def officeDetailUrl(referenceNumber: String) = withBaseUrl(s"offices/$referenceNumber")

  def formatDateWithTime(instant: Option[Instant]): String =
    instant.fold("")(i => DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(i))
  def formatLocalDate(date: LocalDate): String         = DateTimeFormatter.ISO_DATE.format(date)
  def formatLocalTime(time: LocalTime): String         = DateTimeFormatter.ISO_TIME.format(time)
  def formatLocalTime(time: Option[LocalTime]): String = time.fold("")(t => formatLocalTime(t))

  val defaultVal = "-"
  def valueOrDefault(value: String, default: String = defaultVal): String =
    if (value.trim.isEmpty()) default else value
  def optionOrDefault[A](value: Option[A], default: String = defaultVal): String =
    value.fold(default)(_.toString())
  def displayBool(value: Boolean): String = if (value) "True" else "False"
}
