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
    "firstName" -> firstNames(Random.nextInt(firstNames.length)),
    "siret"     -> Random.alphanumeric.filter(_.isDigit).take(9).mkString,
    "number"    -> Random.nextInt(1000000)
  ))

  val scn = scenario("Submit a report without attachment")
    .feed(randomFeeder)
    .exec(http("Send report")
      .post("/api/reports")
      .body(StringBody("""
      {
        "category":"Café / Restaurant",
        "subcategories":["Hygiène","J'ai vu un animal (nuisible)"],
        "companyName":"OLA PIZZ",
        "companyAddress":"OLA PIZZ - 7 B RUE DES COURANCES - 37500 CHINON",
        "companyPostalCode":"37500",
        "companySiret":"83493288100016",
        "firstName":"${firstName}",
        "lastName":"Doe${number}",
        "email":"doe${number}@example.com",
        "contactAgreement":false,
        "employeeConsumer":false,
        "files":[],
        "details":[
          {"label":"Date du constat :","value":"05/12/2019"},
          {"label":"Quel animal avez-vous vu&#160;:","value":"Rongeurs (rat, souris...)"},
          {"label":"Où l'avez-vous vu&#160;:","value":"Dans mon plat"}
        ]}
      """)).asJson
    )

  setUp(scn.inject(atOnceUsers(2)).protocols(httpProtocol))
}
