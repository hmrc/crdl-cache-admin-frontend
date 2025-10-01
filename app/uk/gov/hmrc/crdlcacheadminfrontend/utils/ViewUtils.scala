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
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import java.time.LocalDate
import java.time.LocalTime

object ViewUtils {
    def pageTitle(title: String)(implicit messages: Messages) =
        messages("page.title", title)

    val baseUrl = "crdl-cache"

    def withBaseUrl(url: String): String = s"/$baseUrl/$url"

    val listsUrl = withBaseUrl("lists")
    def listDetailUrl(code: String) = withBaseUrl(s"lists/$code")

    val officesUrl = withBaseUrl("offices")
    def officeDetailUrl(referenceNumber: String) = withBaseUrl(s"offices/$referenceNumber")

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss").withZone(ZoneId.of("UTC"))
    def formatDateWithTime(instant: Option[Instant]): String = instant.fold("")(i => dateTimeFormatter.format(i))
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    def formatLocalDate(date: LocalDate): String = dateFormatter.format(date)
    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
    def formatLocalTime(time: LocalTime): String = timeFormatter.format(time)
    def formatLocalTime(time: Option[LocalTime]): String = time.fold("")(t => formatLocalTime(t))


    def codeListDetailsLink(code: String): HtmlContent = HtmlContent(
        s"""
            <a href="${{listDetailUrl(code)}}">
                Details
                <span class="govuk-visually-hidden">for code list $code</span>
            </a>
        """
    )

    def customOfficeDetailsLink(referenceNumber: String): HtmlContent = HtmlContent(
        s"""
            <a href="${{officeDetailUrl(referenceNumber)}}">
                Details
                <span class="govuk-visually-hidden">for customs office $referenceNumber</span>
            </a>
        """
    )

    def formatCodeListProperties(properties: Map[String, String]) = {
        s"""
            ${properties.map { case (k, v) => s"<p class='govuk-!-margin-0'>$k => $v</p>"}.mkString("")}
        """
    }

    def formatOfficeAddress(address: String, city: String, postcode: String) = {
        s"""
            <div>$address</div>
            <div>$city $postcode</div>
        """
    }

    val defaultVal = "-"

    def valueOrDefault(value: String, default: String = defaultVal): String = if (value.trim.isEmpty()) default else value
    def optionOrDefault[A](value: Option[A], default: String = defaultVal): String = value.fold(default)(_.toString())
    def displayBool(value: Boolean): String = if (value) "True" else "False"
}
