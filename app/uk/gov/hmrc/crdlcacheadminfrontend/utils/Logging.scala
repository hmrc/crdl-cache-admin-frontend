/*
 * Copyright 2023 HM Revenue & Customs
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

import org.slf4j.{Logger, LoggerFactory}
import play.api.{LoggerLike, MarkerContext}
import sourcecode.{Line, Name}

trait Logging {

  private lazy val loggerName: String = this.getClass.getName.stripSuffix("$")
  private lazy val className: String  = this.getClass.getSimpleName.stripSuffix("$")
  private val baseLogger: LoggerLike = new LoggerLike {
    override val logger: Logger = LoggerFactory.getLogger(loggerName)
  }

  val logger: EnhancedLogger = new EnhancedLogger(baseLogger, className)
}

class EnhancedLogger(base: LoggerLike, className: String) extends LoggerLike {
  override val logger: Logger = base.logger
  private def prefixLog(msg: String, name: Name, line: Line): String = {
    s"[$className][${name.value}]:[${line.value}]${if (msg.startsWith("[")) msg else " " + msg}"
  }

  def debug(
    message: => String
  )(implicit mc: MarkerContext, method: Name, line: Line): Unit =
    base.debug(prefixLog(message, method, line))

  def info(
    message: => String
  )(implicit mc: MarkerContext, method: Name, line: Line): Unit =
    base.info(prefixLog(message, method, line))

  def warn(
    message: => String
  )(implicit mc: MarkerContext, method: Name, line: Line): Unit =
    base.warn(prefixLog(message, method, line))

  def error(
    message: => String
  )(implicit mc: MarkerContext, method: Name, line: Line): Unit =
    base.error(prefixLog(message, method, line))

  def debug(message: => String, e: => Throwable)(implicit
    mc: MarkerContext,
    method: Name,
    line: Line
  ): Unit = base.debug(prefixLog(message, method, line), e)

  def info(message: => String, e: => Throwable)(implicit
    mc: MarkerContext,
    method: Name,
    line: Line
  ): Unit = base.info(prefixLog(message, method, line), e)

  def warn(message: => String, e: => Throwable)(implicit
    mc: MarkerContext,
    method: Name,
    line: Line
  ): Unit = base.warn(prefixLog(message, method, line), e)

  def error(message: => String, e: => Throwable)(implicit
    mc: MarkerContext,
    method: Name,
    line: Line
  ): Unit = base.error(prefixLog(message, method, line), e)

}
