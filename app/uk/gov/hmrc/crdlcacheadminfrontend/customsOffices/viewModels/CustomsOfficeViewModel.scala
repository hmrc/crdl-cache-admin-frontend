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

package uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.viewModels

import java.time.Instant
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.LocalTime

final case class CustomsOfficeViewModel(
    referenceNumber: String,
    officeUsualName: String,
    activeFrom: Option[Instant],
    activeTo: Option[Instant],

    location: Location,
    contact: Contact,
    trader: Trader,
    specificInfo: SpecificInfo,
    references: References,
    details: Details,
    timetables: List[Timetable]

)

final case class Location(
    city: String,
    country: String,
    region: Option[String],
    language: String
)

final case class Contact(
    streetAndNumber: String,
    postalCode: String,
    phoneNumber: Option[String],
    faxNumber: Option[String],
    telexNumber: Option[String],
    emailAddress: Option[String]
)

final case class Trader(
    traderDedicated: Boolean,
    dedicatedTraderName: Option[String],
    dedicatedTraderLanguageCode: Option[String]
)

final case class SpecificInfo(
    nctsEntryDate: Option[LocalDate],
    unLocodeId: Option[String],
    geoInfoCode: Option[String],
    notes: String
)

final case class References(
    numberHigherAuthority: Option[String],
    numberMainOffice: Option[String],
    numberCompetentAuthorityOfEnquiry: Option[String],
    numberCompetentAuthorityOfRecovery: Option[String],
    nearestOffice: Option[String],
    numberTakeover: Option[String]
)

final case class Details(
    prefixSuffixFlag: Boolean,
    prefixSuffixLevel: Option[String],
    prefixSuffixName: Option[String],
    spaceToAdd: Boolean
)

final case class Timetable(
    seasonCode: Int,
    seasonName: Option[String],
    seasonStartDate: LocalDate,
    seasonEndDate: LocalDate,
    timetableLines: List[TimetableLine]
)

final case class TimetableLine(
    dayInTheWeekBeginDay: DayOfWeek,
    dayInTheWeekEndDay: DayOfWeek,
    openingHoursTimeFirstPeriodFrom: LocalTime,
    openingHoursTimeFirstPeriodTo: LocalTime,
    openingHoursTimeSecondPeriodFrom: Option[LocalTime],
    openingHoursTimeSecondPeriodTo: Option[LocalTime],
    customsOfficeRoleTrafficCompetence: List[Competance]
)

final case class Competance(
    roleName: String,
    trafficType: String
)
