package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

class BasicSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("https://app-513c11a7-052f-4ff1-8670-5a17dacb7327.cleverapps.io")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val firstNames = Array("John", "Jane", "Margaret", "Georges", "Kate")

  val randomFeeder = Iterator.continually(Map(
    "firstName"     -> firstNames(Random.nextInt(firstNames.length)),
    "companySiret"  -> Random.alphanumeric.filter(_.isDigit).take(9).mkString,
    "randNumber"    -> Random.nextInt(1000000),
    "uploadResult"  -> ""
  ))

  val reportWithoutAttachment = scenario("Submit a report without attachment")
    .feed(randomFeeder)
    .exec(http("Send report")
      .post("/api/reports")
      .body(ElFileBody("requests/report.json")).asJson
    )

  def withAttachment(size: String) = scenario(s"Submit a report with a ${size} attachment")
    .feed(randomFeeder)
    .exec(http("Upload file")
      .post("/api/reports/files")
      .formUpload("reportFile", s"attachments/${size}.jpg")
      .check(
        status.is(200),
        bodyString.saveAs("uploadResult")
      )
    )
    .pause(200 milliseconds, 3 seconds)
    .exec(
      http("Send report")
      .post("/api/reports")
      .body(ElFileBody("requests/report.json")).asJson
    )

  setUp(
    reportWithoutAttachment.inject(rampUsers(1000) during (100 seconds)),
    withAttachment("small").inject(rampUsers(500) during (100 seconds)),
    withAttachment("large").inject(rampUsers(50) during (100 seconds))
  ).protocols(httpProtocol)
}
