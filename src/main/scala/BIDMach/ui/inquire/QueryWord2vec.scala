package BIDMach.ui.inquire

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex
import java.util.Calendar//to get the timestamp
import java.text.SimpleDateFormat
import BIDMat.{CMat,CSMat,DMat,Dict,FMat,FND,GMat,GDMat,GIMat,GLMat,GSMat,GSDMat,GND,HMat,IDict,Image,IMat,LMat,Mat,SMat,SBMat,SDMat,TMat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMach.datasources._
import BIDMach._


class QueryWord2vec {
    
    Mat.checkMKL
    Mat.checkCUDA
    Mat.useCache= true
    
    var corpus = 0;  // select 1-for LJ; 0-for Google

    val resultsDir = "./foodtype/";
    val sentsDataDir = "/data/livejournal/sentences/";
    
    //matrix correspond to old dictionary and google embedding
    val w2vMatFile = sentsDataDir + (if (corpus == 0 ) "googleEmbeddings.fmat" else "LJEmbeddings.fmat")
    
    var w2vMat = GMat(loadFMat(w2vMatFile).t);  // 300xnrWords
    
    val dictFile = sentsDataDir + "masterDict.sbmat";
    var dict = loadDict(dictFile);
    
    //load user dictionary
    var userDict = loadDict("/data/livejournal/userDict.sbmat", pad=false);
    
    def loadDict(filename: String,pad:Boolean=false) = {
        val a = loadSBMat(filename)
        Dict(a,irow(0->a.ncols))
    }
    
    def make_query_vec( query : String) : FMat = {
    
      var query_vec = FMat(size(w2vMat, 1), 1);
    
    
    	// Converts input query to dictionary indexes
    	var ss = query.toLowerCase().split(" ")
    
    	var str = "";
    
    	val weights = Array.fill(ss.length+1){1.0}; // Create a weight vector
    	for (i <- 0 until ss.length) {
    	  str = ss(i).toLowerCase();
    	  if (str(0) == '[' && str(str.length - 1) == ']') {
    		// Convert weight inside the brackets into a double
    		weights(i) = (str.stripPrefix("[").stripSuffix("]").trim).toDouble;
    		ss(i) = null;
    	  }
    	}
    
    	// Convert input query to a word2vec vector
    	var s = "";
    	for(i <- 0 until ss.length) {
    	  if(ss(i) != null) {
    		s = ss(i).toLowerCase();
    
    		if(dict(s) == -1) {
    		  printf("WARNING: did not find %s in master dict\n", s);
    		} else {
    		  var vec = FMat(w2vMat(?, 1+dict(s)));
    
    		  if(sum(vec^2)(0) == 0) {
    			// printf("WARNING: %s is not in google wordvec database\n", s);
    		  } else {
    			// printf("adding %s to vector\n", s);
    			query_vec += vec * weights(i+1);
    		  }
    		}
    	  }
    	}
      // Normalize
      // size 300x1
      query_vec = query_vec / norm(query_vec);
    
      return(query_vec);
    }
    
    def getSFDS(filename:String, nFiles: Int) = {
        val opts = new SFileSource.Options
        opts.fnames = List(FileSource.simpleEnum(filename, 1, 0))
        opts.batchSize = 100000;//500000;
        opts.nend = nFiles
        opts.eltsPerSample = 30  //Upper bound of the average sentence length
        implicit val threads = threadPool(4)
        val ds = new SFileSource(opts)
        ds.init
        ds
    }
    
    def getFDS(filename:String, nFiles: Int) = {
        val opts = new FileSource.Options
        opts.fnames = List(FileSource.simpleEnum(filename, 1, 0))
        opts.batchSize = 100000;//500000;
        opts.nend = nFiles
        implicit val threads = threadPool(4)
        val ds = new FileSource(opts)
        ds.init
        ds
    }
    
    var nFiles = 10
    
    def query( query_s : String , top : Int, filter: String = "", minWords: Int = 5, maxWords : Int = 40)={
    	//information about the query
    	//date
    	val today = Calendar.getInstance().getTime();
    	val dataFormat = new SimpleDateFormat("yyyy.MM.dd.k:mm");
    	val currentTime = dataFormat.format(today);
    	//query to vector
    	val query_vec = make_query_vec(query_s);
    	var pFiles = 1.0;
    	/*println("how much percentage of data would you like to load?(range from 0.0 to 1.0)");
    	val pFileStr = readLine();
    	if(pFileStr!="") pFiles = pFileStr.toDouble; 
    	var nFiles = 1;
    	if(pFiles<=1.0 && pFiles>=0){
    		println("running on "+pFiles+" of data...");
    		nFiles =(math.floor(pFiles*nFiles)).toInt;
    	}
    	else{
    		println("WARNING: percentage is not valid, will run on full data instead");
    	}*/
    	//save queryhistory
    	//val fw0 = new FileWriter("./foodtype/privacy_queryhistory_full_top.txt",true);
    	/*val fw0 = new FileWriter("./foodtype/queryhistory_compare.txt",true);
    	val bw0 = new BufferedWriter(fw0);
    	val out0 = new PrintWriter(bw0);
    	println(query_s+'\t'+filter.toString+'\t'+top.toString+'\t'+minWords.toString+'\t'+maxWords.toString+'\t'+nFiles.toString+'\t'+currentTime);
    	out0.println(query_s+'\t'+filter.toString+'\t'+top.toString+'\t'+minWords.toString+'\t'+maxWords.toString+'\t'+nFiles.toString+'\t'+currentTime);
    	out0.close();*/
    	//filter
    	var filterRegex = new Regex(filter);
    	if(filter=="") filterRegex = null;
    	//initialize the result saving part
    	var outResFMat = DMat(ones(1,1));
    	var outSentsSMat = csrow("content");//1sentence before an after the retrieved sentence
    	var outURLsSMat = csrow("url");
    	var outSentMat = csrow("sentence");
    	/*val fw2 = new FileWriter("./foodtype/queryresult_full_Aug.txt",false);
    	val bw2 = new BufferedWriter(fw2);
    	val out2 = new PrintWriter(bw2);*/
    	
    	//start to scan the whole file
    	var sentFile = sentsDataDir + "data%d_sent.smat.lz4";
    	var bowFile = sentsDataDir + "data%d.smat.lz4";
    	val idFile = sentsDataDir + "data%d.imat";
    	val dds = getSFDS(sentFile,nFiles)
    	val bds = getSFDS(bowFile,nFiles)
    	val ifds = getFDS(idFile,nFiles)
    	var lt = 0f
    	var mt = 0f
    	var t1 = 0f 
    	var t2 = 0f
    	var counter=0;
    	var top_low=0.0;
    	/*var x_ = FMat(ones(1,1));
    	var bestIndex = IMat(ones(1,1));*/
    	while (dds.hasNext){
    		//println(counter)
    		tic
    		val sents = (dds.next)(0).asInstanceOf[SMat]
    		val data = (bds.next)(0).asInstanceOf[SMat]
    		val ids = (ifds.next)(0).asInstanceOf[IMat]
    		lt+=toc
    			
    	//get the sentence vector by multiplying w2vMat with data(seems to be one hot vectors)
    	  tic
    	  val magic = w2vMat* GSMat(data)
    	  mt+=toc
    	  tic
    	  magic ~ magic / (sqrt(magic dot magic)+1e-7f)
    
    		// perform query
    		var res = ((query_vec.t) * magic);  // 1x#sentences
    		 t1+=toc;    
    	// Sort Results to Return Top Ones
    		var (x_, bestIndex_) = sortdown2(res.t);
    		val x = FMat(x_)
    		val bestIndex = IMat(bestIndex_)
    			
    	//    println(x(0->10))
    		tic
    		var i = 0
    		var count = 0;
    		var prev_res = -1f;
    		while(count < top && i < res.length && x(i)>=top_low) {
    		  var ix = bestIndex(i)
    
    		  var curr = IMat(FMat(sents(find(sents(?, ix)), ix)));//find linear indices of none zeros of sents' ix th col
    		  var z = dict(curr-1).t;
    		  var sent = (z ** csrow(" ")).toString().replace(" ,", " ");
    		  var preSent="";//sentence before the retrieved one
    		  var postSent="";//sentence after the retrieved one
    		  var curid = ids(0,ix)
    		  var userid = userDict(curid)
    	//      println(sent)
    				
    		  var numWords = z.length;
    		//try id
    		
    		  ///if(x(i) != prev_res && // discard repeated strings?//seems to assume different sentences won't have same score????
    		  if(numWords >= minWords && numWords <= maxWords &&// minimum words
    			(filterRegex== null || filterRegex.findFirstIn(sent) == None) // filter for words
    		  ) {
    				/*if(count==0){
    				printf("%.3f -- %s\n", res(ix), sent);
    				}*/
    			 if(ix-1>=0){
    				if(ids(2,ix-1)==ids(2,ix)){
    					var prer=IMat(FMat(sents(find(sents(?, ix-1)), ix-1)));
    					var zz = dict(prer-1).t;
    					preSent =  (zz ** csrow(" ")).toString().replace(" ,", " ");
    				}
    			}
    			if(ids.ncols>ix+1){
    				if(ids(2,ix+1)==ids(2,ix+1)){
    				  var postr=IMat(FMat(sents(find(sents(?, ix+1)), ix+1)));
    				  var zz = dict(postr-1).t;
    				  postSent =  (zz ** csrow(" ")).toString().replace(" ,", " ");
    				  }
    			}
    				outSentsSMat = outSentsSMat on (preSent+sent+postSent);
    				outResFMat = outResFMat on x(i);
    				outURLsSMat = outURLsSMat on ("http://" + userid + ".livejournal.com/");
    				outSentMat = outSentMat on sent;
    				count += 1;
    				/*out2.println(sent +'\t'+res(ix).toString+'\t'+currentTime+'\t'+"http://" + userid + ".livejournal.com/");*/
    		  }  
    		  i += 1;
    		}                     
    		t2+=toc;
    		//sort the result		
    		/*println("before sorting")
    		println(outSentsSMat.nrows);
    		println(outResFMat.nrows);
    		println(outURLsSMat.nrows);*/
    
    		var(x0_,bestIndex0) = sortdown2(outResFMat);
    		outResFMat =  outResFMat(bestIndex0,0);
    		outSentsSMat = outSentsSMat(bestIndex0,0);
    		outURLsSMat = outURLsSMat(bestIndex0,0);
    		outSentMat = outSentMat(bestIndex0,0);
    		outResFMat = outResFMat(0->(top+1),0);
    		outSentsSMat = outSentsSMat(0->(top+1),0);
    		outURLsSMat = outURLsSMat(0->(top+1),0);
    		outSentMat = outSentMat(0->(top+1),0);
    		//check dimension after sorting
    		/*println("sort fin")
    		println(top+1)
    		println(outSentsSMat.ncols);
    		println(outResFMat.ncols);
    		println(outURLsSMat.ncols);
    		println(outSentsSMat.nrows);
    		println(outResFMat.nrows);
    		println(outURLsSMat.nrows);*/
    		//update the lowest score
    		top_low = outResFMat(top);
    		counter+=1;
    	}
    		
    	println("Total loading: "+lt)
    	println("Total time for sparse matrix mul: "+mt)
    	println("Total t1: "+t1)
    	println("Total t2 (pull out top sentences): "+t2)
    	println("it took " + (lt+mt+t1+t2) + " seconds to run on "+pFiles+" of data");
    	//tic;
    	// save results
    	//println("saving results...")
    	var(x_,bestIndex) = sortdown2(outResFMat);
    	outResFMat =  outResFMat(bestIndex);
    	outSentsSMat = outSentsSMat(bestIndex);
    	outURLsSMat = outURLsSMat(bestIndex);
    	outSentMat = outSentMat(bestIndex);
    	var results = ""
    	for(i <-0 until top+1){
    		println(outResFMat(i).toString+'\t'+outSentMat(i)+'\t'+outURLsSMat(i))
    		results += outResFMat(i).toString+'\t'+outSentMat(i)+'\t'+outURLsSMat(i)+"\n"
    		//out1.println(outResFMat(i).toString+'\t'+outSentMat(i)+'\t'+outSentsSMat(i)+'\t'+currentTime+'\t'+outURLsSMat(i));
    	}
    	//save to txt file
    	/*println("save to txt")
    	//val fw1 = new FileWriter("./foodtype/privacy_queryresult_full_top.txt",false);
    	val fw1 = new FileWriter("./foodtype/queryresult_compare.txt",false);
    	val bw1 = new BufferedWriter(fw1);
    	val out1 = new PrintWriter(bw1);
    	for(i <-0 until top+1){
    		println(outResFMat(i).toString+'\t'+outSentMat(i)+'\t'+outURLsSMat(i))
    		out1.println(outResFMat(i).toString+'\t'+outSentMat(i)+'\t'+outSentsSMat(i)+'\t'+currentTime+'\t'+outURLsSMat(i));
    	}
    	out1.close();*/
    	//save to hdf5 file
    	/*println("save to hdf5")
    	saveAs("./foodtype/privacy_scores_full_top.mat",  outResFMat(0->top),"scores");
    	saveAs("./foodtype/privacy_contexts_full_top.mat",outSentsSMat(0->top),"contexts");
    	saveAs("./foodtype/privacy_urls_full_top.mat",outURLsSMat(0->top),"urls");*/
    	//toc
    	results
    	//println("it took " + toc + " seconds to save data");
    	//println("done!")
    }
}

object QueryWord2vec {
    def main(args:Array[String]) {
        val q = new QueryWord2vec
        //q.query("I feel good today",10)
    }
}