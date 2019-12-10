package signalconso

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

/*
 * App : https://app-721f65f7-5c97-43b6-b753-31b3af32745a.cleverapps.io
 * Api : https://app-513c11a7-052f-4ff1-8670-5a17dacb7327.cleverapps.io
 */

class BasicSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("https://app-513c11a7-052f-4ff1-8670-5a17dacb7327.cleverapps.io")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val firstNames = Array("John", "Jane", "Margaret", "Georges", "Kate")

  val reportFeeder = Iterator.continually(Map(
    "firstName"     -> firstNames(Random.nextInt(firstNames.length)),
    "companySiret"  -> Random.alphanumeric.filter(_.isDigit).take(9).mkString,
    "randNumber"    -> Random.nextInt(1000000),
    "uploadResult"  -> ""
  ))

  val reportWithoutAttachment = scenario("Déclaration d'un signalement sans PJ")
    .feed(reportFeeder)
    .exec(http("Envoi du signalement")
      .post("/api/reports")
      .body(ElFileBody("requests/report.json")).asJson
    )

  def withAttachment(size: String) = scenario(s"Déclaration d'un signalement avec une PJ (${size})")
    .feed(reportFeeder)
    .exec(http(s"Envoi d'une PJ au signalement (${size})")
      .post("/api/reports/files")
      .formUpload("reportFile", s"attachments/${size}.jpg")
      .check(
        status.is(200),
        bodyString.saveAs("uploadResult")
      )
    )
    .pause(200 milliseconds, 3 seconds)
    .exec(
      http("Envoi du signalement")
      .post("/api/reports")
      .body(ElFileBody("requests/report.json")).asJson
    )
  
  val ccrfBasic = scenario("Un agent CCRF se connecte et consulte le dernier signalement")
    .exec(http("Login de l'agent")
      .post("/api/authenticate")
      .body(StringBody("""
      {
        "login": "test.dgccrf@signalconso.beta.gouv.fr",
        "password": "test"
      }
      """)).asJson
      .check(jsonPath("$.token").saveAs("authToken"))
    )
    .doIf("${authToken.exists()}") {
      exec(http("Liste des 20 derniers signalements")
        .get("/api/reports?offset=0&limit=20")
        .header("X-Auth-Token", "${authToken}")
        .check(jsonPath("$.entities[0].id").saveAs("reportId"))
      )
    }
    .doIf("${reportId.exists()}") {
      exec(http("Consultation du dernier signalement")
        .get("/api/reports/${reportId}")
        .header("X-Auth-Token", "${authToken}")
        .check(
          jsonPath("$.files[0].id").optional.saveAs("reportFileId"),
          jsonPath("$.files[0].filename").optional.saveAs("reportFilename")
        )
      )
      .exec(http("Consultation des évènements du signalement")
        .get("/api/reports/${reportId}/events")
        .header("X-Auth-Token", "${authToken}")
      )
      .doIf("${reportFileId.exists()}") {
        exec(
          http("Consultation d'une pièce-jointe")
          .get("/api/reports/files/${reportFileId}/${reportFilename}")
          .check(md5.in("fb167b7ee93c54cf92250f29654f95f0", "314eaf7f6b1783510fd12ac893e63063"))
        )
      }
    }

  setUp(
    ccrfBasic.inject(rampUsers(10) during (5 seconds)),
    reportWithoutAttachment.inject(rampUsers(10) during (5 seconds)),
    withAttachment("small").inject(rampUsers(10) during (5 seconds)),
    withAttachment("large").inject(rampUsers(5) during (5 seconds))
  ).protocols(httpProtocol)
}
