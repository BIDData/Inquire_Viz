package BIDMach.ui

import BIDMat.{Mat,FMat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMach._
import BIDMach.models._
import BIDMach.datasources._
import BIDMach.datasinks._

import play.api._
import play.api.routing.Router
import play.api.routing._
import play.api.routing.sird._
import play.api.mvc._
import play.core.server.{ServerConfig, NettyServer}
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._


/**
 * The UIServer class for BIDMach Engine
 * It accepts a Learner and an Options as parameter and create the server for Interactive Machine Learning
 * 
 * 
 **/
 

class UIServer(nn: Learner,opts: Learner.Options) extends LocalWebServer{
    
    val modelName = nn.model.getClass.getSimpleName
    val mopts = opts.asInstanceOf[Model.Opts]
    val paras = utils.getOptions(opts)
    var channel: Concurrent.Channel[String] = null
    
    override def routes = {
        case GET(p"/")=>
          controllers.Assets.at(path="/public", file="index.html")
        case GET(p"/meta")=>Action {
            Results.Ok(Json.obj(
                "type" -> "meta",
                "model" -> modelName,
                "parameters" -> JsObject(paras.map(x=>x._1 -> JsString(x._2))),
                "metrics" -> (mopts.logDataSink match { case s:NetSink=>s.opts.names})
            ))
        }
        case GET(p"/register/")=>Action {
            Results.Ok("OK")
        }
        case GET(p"/assets/$file*")=>
          controllers.Assets.at(path="/public", file=file)
        case GET(p"/socket")=>
           WebSocket.using[String] { 
               request => 
                // Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
                val (out, channel) = Concurrent.broadcast[String]
                // log the message to stdout and send response back to client
                val in = Iteratee.foreach[String] {
                  msg =>
                    println(msg)
                    // the Enumerator returned by Concurrent.broadcast subscribes to the channel and will
                    // receive the pushed messages
                    //channel push("I received your message: " + msg)
                }
                mopts.logDataSink match {
                    case s:NetSink =>
                        s.opts.channel = new UIServer.Channel(channel)
                }
                (in,out)
              }
    }
}

object UIServer {
    
    def start(nn: Learner,opts: Learner.Options, ns: Array[String] = null) {
        Mat.checkMKL
        Mat.checkCUDA
        Mat.useCache = true
        val mopts = opts.asInstanceOf[Model.Opts]
        val inferns = if (mopts.logFuncs == null) Array[String]() else (0 until mopts.logFuncs.length).map("log"+_).toArray
        //val ns = if (mopts.logFuncs == null)  Array[String]() else mopts.logFuncs.zipWithIndex.map(x=>Logging.getFuncName(x._1,x._2))
        mopts.logDataSink = new NetSink(new NetSink.Options{ofcols=2000;names= if (ns == null) inferns else ns})
        val server = new UIServer(nn,opts)
    }
    
    class Channel(channel: Concurrent.Channel[String]) extends NetSink.Channel {
        def push(names:Array[String],mats:Array[Mat]){
            val res = Json.obj(
                "type"  -> "data",
                "data" -> names.zip(mats.map(mean(_,2))).map(x=>Json.obj("name"->x._1,
                                                          "dim"->List(x._2.nrows,x._2.ncols),
                                                          "data"->FMat(x._2).data))
            )
            channel.push(res.toString)
        }
    }
    
    def main(args: Array[String]) {
        val (nn,opts) = KMeans.learner(rand(100,1000))
        opts.logFuncs = Array(Logging.loss(nn),Logging.test)
        opts.npasses = 10000
        start(nn,opts,Array("loss","test"))
        nn.train
    }

    
}
