package BIDMach.ui.inquire

import play.api._
import play.api.routing.Router
import play.api.routing._
import play.api.routing.sird._
import play.api.mvc._
import play.core.server.{ServerConfig, NettyServer}
import play.api.mvc._
import play.api.libs.json._
import BIDMach.ui.LocalWebServer


/**
 * The Server for inquire project
 * 
 * */

class InquireServer extends LocalWebServer{
    
    val q = new QueryWord2vec

    override def routes = {
        case GET(p"/")=>
          controllers.Assets.at(path="/inquire/frontend", file="index.html")
        case GET(p"/query" ? q_o"data=$data" & q_o"nFiles=$nFiles") => Action {
            val d = data.getOrElse("")
            val n = nFiles.getOrElse("10").toInt
            q.nFiles = n
            Results.Ok(JsObject(Seq(                
                "type" -> JsString("result"),
                "data" -> JsString(q.query(d,20)))))
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


