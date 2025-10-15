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

package uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.models

import play.api.libs.json.*
import java.time.Instant
import java.time.LocalDate
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.viewModels.*

final case class CustomsOffice(
  referenceNumber: String,
  activeFrom: Option[Instant],
  activeTo: Option[Instant],
  referenceNumberMainOffice: Option[String],
  referenceNumberHigherAuthority: Option[String],
  referenceNumberCompetentAuthorityOfEnquiry: Option[String],
  referenceNumberCompetentAuthorityOfRecovery: Option[String],
  referenceNumberTakeover: Option[String],
  countryCode: String,
  emailAddress: Option[String],
  unLocodeId: Option[String],
  nctsEntryDate: Option[LocalDate],
  nearestOffice: Option[String],
  postalCode: String,
  phoneNumber: Option[String],
  faxNumber: Option[String],
  telexNumber: Option[String],
  geoInfoCode: Option[String],
  regionCode: Option[String],
  traderDedicated: Boolean,
  dedicatedTraderLanguageCode: Option[String],
  dedicatedTraderName: Option[String],
  customsOfficeSpecificNotesCodes: List[String],
  customsOfficeLsd: CustomsOfficeDetail,
  customsOfficeTimetable: List[CustomsOfficeTimetable]
)

object CustomsOffice {
  given Reads[CustomsOffice] = Json.reads[CustomsOffice]

  def toViewModel(value: CustomsOffice): CustomsOfficeViewModel = CustomsOfficeViewModel(
    referenceNumber = value.referenceNumber,
    officeUsualName = value.customsOfficeLsd.customsOfficeUsualName,
    activeFrom = value.activeFrom,
    activeTo = value.activeFrom,
    location = Location(
      city = value.customsOfficeLsd.city,
      country = value.countryCode,
      region = value.regionCode,
      language = value.customsOfficeLsd.languageCode
    ),
    contact = Contact(
      streetAndNumber = value.customsOfficeLsd.streetAndNumber,
      postalCode = value.postalCode,
      phoneNumber = value.phoneNumber,
      faxNumber = value.faxNumber,
      telexNumber = value.telexNumber,
      emailAddress = value.emailAddress
    ),
    trader = Trader(
      traderDedicated = value.traderDedicated,
      dedicatedTraderName = value.dedicatedTraderName,
      dedicatedTraderLanguageCode = value.dedicatedTraderLanguageCode
    ),
    specificInfo = SpecificInfo(
      nctsEntryDate = value.nctsEntryDate,
      unLocodeId = value.unLocodeId,
      geoInfoCode = value.geoInfoCode,
      notes = value.customsOfficeSpecificNotesCodes.mkString(", ")
    ),
    references = References(
      numberHigherAuthority = value.referenceNumberHigherAuthority,
      numberMainOffice = value.referenceNumberMainOffice,
      numberCompetentAuthorityOfEnquiry = value.referenceNumberCompetentAuthorityOfEnquiry,
      numberCompetentAuthorityOfRecovery = value.referenceNumberCompetentAuthorityOfRecovery,
      nearestOffice = value.nearestOffice,
      numberTakeover = value.referenceNumberTakeover
    ),
    details = Details(
      prefixSuffixFlag = value.customsOfficeLsd.prefixSuffixFlag,
      prefixSuffixLevel = value.customsOfficeLsd.prefixSuffixLevel,
      prefixSuffixName = value.customsOfficeLsd.prefixSuffixName,
      spaceToAdd = value.customsOfficeLsd.spaceToAdd
    ),
    timetables = value.customsOfficeTimetable.map(cot =>
      Timetable(
        seasonCode = cot.seasonCode,
        seasonName = cot.seasonName,
        seasonStartDate = cot.seasonStartDate,
        seasonEndDate = cot.seasonEndDate,
        timetableLines = cot.customsOfficeTimetableLine.map(cotLine =>
          TimetableLine(
            dayInTheWeekBeginDay = cotLine.dayInTheWeekBeginDay,
            dayInTheWeekEndDay = cotLine.dayInTheWeekEndDay,
            openingHoursTimeFirstPeriodFrom = cotLine.openingHoursTimeFirstPeriodFrom,
            openingHoursTimeFirstPeriodTo = cotLine.openingHoursTimeFirstPeriodTo,
            openingHoursTimeSecondPeriodFrom = cotLine.openingHoursTimeSecondPeriodFrom,
            openingHoursTimeSecondPeriodTo = cotLine.openingHoursTimeSecondPeriodTo,
            customsOfficeRoleTrafficCompetence = cotLine.customsOfficeRoleTrafficCompetence.map(
              rtc => Competance(rtc.roleName, rtc.trafficType)
            )
          )
        )
      )
    )
  )
}
