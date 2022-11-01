package dpla.monthly_stats_collector

import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.github.tototoshi.csv._
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client


import java.io.StringWriter
import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.io.Source
import scala.util.{Failure, Success, Using}


object Main extends App {

  val apiKey = args(0)
  val s3Bucket = args(1)

  val now = ZonedDateTime.now(ZoneId.of("UTC")).toInstant
  val timestamp = DateTimeFormatter.ISO_INSTANT.format(now)

  val url =
    "https://api.dp.la/v2/items" +
      "?facets=provider.name" +
      "&facet_size=10000" +
      "&page_size=0" +
      "&api_key=" + apiKey

  val jsonString =
    Using(Source.fromURL(url)) {
      source => source.mkString
    } match {
      case Success(string) => string
      case Failure(e) =>
        throw new RuntimeException("Couldn't load ES result.", e)
    }

  val data = for {
    JObject(json) <- parse(jsonString)
    JField("facets", JObject(facets)) <- json
    (facetName, JObject(fields)) <- facets
    if facetName == "provider.name"
    (fieldName, JArray(fieldValues)) <- fields
    if fieldName == "terms"
    fieldValue <- fieldValues
  } yield fieldValue match {
    case JObject(List(JField("count", JInt(count)), JField("term", JString(name)))) =>
      Seq(timestamp, name, count)
    case JObject(List(JField("term", JString(name)), JField("count", JInt(count)))) =>
      Seq(timestamp, name, count)
    case _ => throw new RuntimeException("Unable to parse api output")
  }

  if (data.isEmpty)
    throw new RuntimeException("Didn't load any records!")

  val csvString = Using(new StringWriter()) {
    stringWriter =>
      val writer = CSVWriter.open(stringWriter)
      writer.writeAll(data)
      writer.close()
      stringWriter.toString
  } match {
    case Success(string) => string
    case Failure(e) =>
      throw new RuntimeException("Couldn't build CSV.", e)
  }

  val s3 = S3Client.builder.region(Region.US_EAST_1).build
  val putObjectRequest = PutObjectRequest.builder()
    .bucket(s3Bucket)
    .key(timestamp)
    .build

  s3.putObject(putObjectRequest, RequestBody.fromString(csvString))
}
