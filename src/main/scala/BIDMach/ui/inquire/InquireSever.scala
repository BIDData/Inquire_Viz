package BIDMach.ui.inquire

import play.api._
import play.api.routing.Router
import play.api.routing._
import play.api.routing.sird._
import play.api.mvc._
import play.core.server.{ServerConfig, NettyServer}
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.ws.ning.NingWSClient
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import BIDMach.ui.LocalWebServer



/**
  * The Server for inquire project
  * 
  * */

class InquireServer extends LocalWebServer{
  
  val q = VecQuery
  var wsClient: NingWSClient = null
  
  override def routes = {
    case GET(p"/")=>
      controllers.Assets.at(path="/inquire/frontend", file="index.html")
    case GET(p"/query" ? q_o"data=$data" & q_o"nFiles=$nFiles" & q_o"minWords=$minWords" & q_o"maxWords=$maxWords" & q_o"top=$top" & q_o"filter=$filter") => Action {
      val d = data.getOrElse("")
      val minW = minWords.getOrElse("5").toInt
      val maxW = maxWords.getOrElse("100").toInt
      val topN = top.getOrElse("100").toInt
      val filterR = filter.getOrElse("")

      // val n = nFiles.getOrElse("10").toInt
      // println("Running query " + d +" with " + n + "files")

      val (scores,sents,moods,urls) = q.query(d,topN,filterR,minW,maxW)
      Results.Ok(Json.obj(
        "result_count" -> scores.length,
        "query" -> d,
        "query_results" -> Json.arr(sents.map(JsString(_))),
        "cosine_similarity" -> scores,
        "emotion" -> Json.arr(moods.map(JsString(_))),
        "url" -> Json.arr(urls.map(JsString(_))),
        "type" -> "result",
        "data" -> sents.mkString("\n")))
    }
    case GET(p"/test2") => Action{
      Results.Ok(s"test2")
    }
    case POST(p"/api/visualize/$api*") => Action.async{ implicit request => {
      if (wsClient == null) wsClient = NingWSClient()
      println(request.body.asJson.toString)
      wsClient.url("http://localhost:8080/api/visualize/"+api)
        .withHeaders("Content-Type" -> "application/json")
        .post(request.body.asJson.getOrElse(Json.obj()).toString)
        .map { response =>
            Results.Ok(response.body)
        }
      }
    }
    case GET(p"/$file*")=>
      controllers.Assets.at(path="/inquire/frontend", file=file)
  }
}

object InquireServer {
  def main(args: Array[String]) {
    val s = new InquireServer
  }
}


