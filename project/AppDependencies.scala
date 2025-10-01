import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "10.1.0"
  private val playVersion = "play-30"
  

  val compile = Seq(
    "uk.gov.hmrc"             %% s"bootstrap-frontend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"             %% s"play-frontend-hmrc-$playVersion" % "12.12.0",
    "uk.gov.hmrc"             %% s"internal-auth-client-$playVersion" % "4.1.0",
    "com.lihaoyi"             %% "sourcecode"                 % "0.4.2"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion    % Test,
    "org.jsoup"               %  "jsoup"                      % "1.13.1"            % Test,
  )

  val it = Seq.empty
}
