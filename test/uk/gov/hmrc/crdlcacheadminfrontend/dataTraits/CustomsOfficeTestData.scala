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

import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.models.CustomsOffice
import java.time.Instant
import uk.gov.hmrc.crdlcacheadminfrontend.customsOffices.models.CustomsOfficeDetail

trait CustomsOfficeTestData {
  /* 
   * Produces a minimul viable Customs Office object for testing.
   * If conditional logic is required for variations please feel free
   * to add paramters and expand 
   */
  def generateCustomsOffice(key: String) = CustomsOffice(
    referenceNumber = s"$key-ReferenceNumber",
    activeFrom = Some(Instant.now()),
    activeTo = Some(Instant.now()),
    referenceNumberMainOffice = None,
    referenceNumberHigherAuthority = None,
    referenceNumberCompetentAuthorityOfEnquiry = None,
    referenceNumberCompetentAuthorityOfRecovery = None,
    referenceNumberTakeover = None,
    countryCode = "CC",
    emailAddress = None,
    unLocodeId = None,
    nctsEntryDate = None,
    nearestOffice = None,
    postalCode = "NE98 1ZZ",
    phoneNumber = None,
    faxNumber = None,
    telexNumber = None,
    geoInfoCode = None,
    regionCode = None,
    traderDedicated = false,
    dedicatedTraderLanguageCode = None,
    dedicatedTraderName = None,
    customsOfficeSpecificNotesCodes = List(),
    customsOfficeLsd = CustomsOfficeDetail(
      customsOfficeUsualName = s"$key Customs Office",
      languageCode = "LC",
      city = s"City of $key",
      prefixSuffixFlag = false,
      prefixSuffixLevel = None,
      prefixSuffixName = None,
      spaceToAdd = false,
      streetAndNumber = s"$key, Benton Park View"
    ),
    customsOfficeTimetable = List()
  )

  def generateCustomsOfficeList(numberOfEntries: Int): List[CustomsOffice] = (1 to numberOfEntries).map(key => generateCustomsOffice(key.toString)).toList

  lazy val defaultCustomsOffice: CustomsOffice = generateCustomsOffice("Default")
  lazy val defaultCustomsOfficeList: List[CustomsOffice] = generateCustomsOfficeList(3)
}
