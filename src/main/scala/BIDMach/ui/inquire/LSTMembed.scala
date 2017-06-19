package BIDMach.ui.inquire

import scala.util.matching.Regex
import BIDMat.{CMat, CSMat, DMat, Dict, FMat, FND, GMat, GDMat, GIMat, GLMat, GSMat, GSDMat, HMat, IDict, Image, IMat, LMat, Mat, SMat, SBMat, SDMat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMach.datasources._
import BIDMach.networks._
import BIDMach._
import scala.collection.mutable.ListBuffer
import java.io._

    
class LSTMembed(dir : String = "/big/livejournal/mercury/destress/") {
    val mdir = dir + "models/"
    val odir = dir + "preds/";  // Directory for input data

    val w = loadSBMat(dir + "sentences2/masterDict.sbmat")
    val dict = Dict(w,irow(0->w.ncols))

    class Eopts extends Learner.Options with SeqToSeq.Opts with MatSource.Opts

    def embed(model:SeqToSeq, m:Mat):(Learner, Eopts) = {   
        val opts = new Eopts;
        opts.copyFrom(model.opts);
        opts.embed = true;
        opts.batchSize = m.ncols//min(m.ncols,128)
        val newmod = new SeqToSeq(opts);
        newmod.refresh = false;
        model.copyTo(newmod);
        implicit val threads = threadPool(4);
        val ds = new MatSource(Array(m),opts)
        val nn = new Learner(
                ds, 
            newmod, 
            null,
            null,
            null,
            opts)

        opts.nvocab = 100000;                     // Vocabulary limit
        opts.npasses = 1;                         // Number of passes over the dataset
        opts.height = 2;                          // Height of the network
        opts.dim = 256;                           // Dimension of LSTM units
        opts.kind = 1;                            // LSTM structure
        opts.netType = 0;                         // Net type (softmax=0, or negsampling=1)
        opts.scoreType = 1;                       // Score type (logloss=0, accuracy=1)
        opts.inwidth = 30;                        // Max input sentence length (truncates)
        opts.outwidth = 30;                       // Max output sentence length (truncates)
        opts.hasBias = true;                      // Use bias terms in linear layers
        opts.pstep = 0.01f;                     // How often to print
        opts.cumScore = 3;                        // Accumulate scores for less-noisy printing
        opts.PADsym = 1;                          // The padding symbol
        opts.OOVsym = 2;                          // The OOV symbol
        opts.STARTsym = 0;
        opts.autoReset=false;
        nn.model.ogmats = Array(grand(opts.dim,opts.batchSize)*0)
        nn.model.omats = Array(rand(opts.dim,opts.batchSize)*0)
        (nn, opts)
      }

    def genSent(s:String) = {
        val data = s.split(" ").filter(_(0)!='[').map(x=>dict(x.toLowerCase)+1).filter(_>=1).reverse
        sparse(irow(0->data.length),irow(data)*0,FMat(irow(data)))
    }

    val model = SeqToSeq.load(mdir+"model256_fit/")

    def genVec(s:String) = {
        val (nn,opts) = embed(model, genSent(s))
        nn.predict  
        val v = nn.model.ogmats(0).asInstanceOf[GMat]
        FMat(v/norm(v))
    }
}
