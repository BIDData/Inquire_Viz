package BIDMach.ui
import BIDMat.{Mat,SBMat,CMat,CSMat,DMat,FMat,IMat,HMat,GMat,GDMat,GIMat,GLMat,GSMat,GSDMat,LMat,SMat,SDMat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMach.datasources._
import BIDMach.datasinks._
import scala.collection.mutable.ListBuffer

class NetSink(override val opts:NetSink.Opts = new NetSink.Options) extends MatSink(opts) { 
  var colsdone = 0;
  
  override def init = { 
    blocks = new ListBuffer[Array[Mat]]();
    setnmats(opts.names.length);
    omats = new Array[Mat](nmats);
    colsdone = 0;
  }
  
  override def put = {
    blocks += omats.map(MatSink.copyCPUmat);
    colsdone += omats(0).ncols;
    if (colsdone >= opts.ofcols) {
      mergeSaveBlocks;
      colsdone = 0;
      blocks = new ListBuffer[Array[Mat]]();
    }
  }

  override def close () = {
    mergeSaveBlocks;
  }
  
  def mergeSaveBlocks = {
    mergeBlocks
    if (blocks.size > 0) {
        if (opts.channel != null)
            opts.channel.push(opts.names,mats)
    }
  }
}

object NetSink {
  trait Channel {
      def push(names:Array[String],mats:Array[Mat])
  }
  
  trait Opts extends MatSink.Opts {
  	var names:Array[String] = null;
  	var ofcols = 100;
  	var channel: Channel = null
  }
  
  class Options extends Opts {

  }
}