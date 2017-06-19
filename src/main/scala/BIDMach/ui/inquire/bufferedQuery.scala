package BIDMach.ui.inquire

import utils._
import memQuery._
import scala.util.matching.Regex
import BIDMat.{CMat, CSMat, DMat, Dict, FMat, FND, GMat, GDMat, GIMat, GLMat, GSMat, GSDMat, HMat, IDict, Image, IMat, LMat, Mat, SMat, SBMat, SDMat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMach.datasources._
import scala.collection.mutable.ListBuffer
import java.io._

    
/**
 * Buffered Query engine for inquire project
 * For word2vec embedding, it first load the sparse sentence data into the main memory, and compute word embedding on the fly
 * For LSTM embedding, it load the embedding data into the main memory 
 * 
 * Parameters: 
 * percData(0.01f): Percent of the data to load. No more than 0.012
 * embedding(0): select 0 for word2vec; 1 for LSTM
 * corpus(0): select 1-for LJ; 0-for Google
 * sentsDataDir 
 */
    

class BufferedQuery(val percData : Float = 0.01f, 
                    val dataDir : String = "/big/livejournal/mercury/destress/",
                    val corpus : Int = 0,
                    val dataset: Int = 0
                   ) {
    Mat.checkMKL
    Mat.checkCUDA
    Mat.useCache= true
    Mat.useGPUcache= true

    // Constants
    val sentsDataDir = dataDir + "sentences2/"
        
    val embedDataDir  = dataDir + "preds/"

    val srcdstDataDir  = dataDir + "srcdst/"

    val seed = 94720;  // default is 94720
                       // Loads, converts to w2v and normalizes    
    val sentsSize = 500;  // Size (nrOfWords) of sentences

    val dictFile = sentsDataDir + "masterDict.sbmat";
    
    val dict = loadDict(dictFile);
    
    val moodDict = loadDict(dataDir + "moods_dict.sbmat.lz4", pad=false);
    
    val usernames = if (dataset ==0 )CSMat(loadSBMat(sentsDataDir + "users_dict_full_values.sbmat.lz4")) else null
    
    val w2vMatFile = sentsDataDir + (if (corpus == 0 ) "googleEmbeddings.fmat" else "LJEmbeddings.fmat")
    
    lazy val w2vMat = loadFMat(w2vMatFile).t;  // 300xnrWords

    lazy val w2vGMat = GMat(w2vMat);  // 300xnrWords
    
    lazy val sent_data = ListBuffer[Mat]()

    lazy val bow_data = ListBuffer[Mat]()

    lazy val id_data = ListBuffer[Mat]()

    lazy val user_data = ListBuffer[Mat]()

    lazy val lstm_sent_data = ListBuffer[Mat]()

    val lstm_embed_data = ListBuffer[Mat]()

    lazy val lstm_id_data = ListBuffer[Mat]()

    lazy val lstm_user_data = ListBuffer[Mat]()
    
    var tmp_bow_data : SMat = null
    
    var tmp_embed_data : FMat = null

    val lstmEmbedObj = if (dataset == 0)new LSTMembed(dataDir) else null

    val listLabels = new File(sentsDataDir).list.filter(_.endsWith("_sent.smat.lz4")).filter(_.startsWith("data"));
    
    val nrDataFiles = listLabels.size;
    
    val nrFiles2Load = floor(nrDataFiles * percData)(0).toInt;

        
    //if (embedding == 0) loadW2V() else loadLSTM() // Call main constructor function
    loadEmbedding()
    
    def getSFDS(filename:String, nFiles: Int) = {
        val opts = new SFileSource.Options
        opts.fnames = List(FileSource.simpleEnum(filename, 1, 0))
        opts.batchSize = 100000;//500000;
        opts.nend = nFiles
        opts.nstart = 0
        opts.eltsPerSample = sentsSize  //Upper bound of the average sentence length
        implicit val threads = threadPool(1)
        val ds = new SFileSource(opts)
        ds.init
        ds
    }
    
    def getFDS(filename:String, nFiles: Int) = {
        val opts = new FileSource.Options
        opts.fnames = List(FileSource.simpleEnum(filename, 1, 0))
        opts.batchSize = 100000;//500000;
        opts.nstart = 0
        opts.nend = nFiles
        implicit val threads = threadPool(1)
        val ds = new FileSource(opts)
        ds.init
        ds
    }
    
    def loadEmbedding(){
        tic
        println("Number of Files: %d. Loading %.3f of them: %d files" format (nrDataFiles,percData,nrFiles2Load));
        
        var sentFile = sentsDataDir + "data%d_sent.smat.lz4";        
    	var bowFile = sentsDataDir + "data%d.smat.lz4";
    	val idFile = sentsDataDir + "data%d.imat";
    	val userFile = sentsDataDir + "data%d_usernamest.imat.lz4";
        
        val lstmSentFile = srcdstDataDir + "src%04d.smat.lz4"
        val embedFile = embedDataDir + "pred%04d.fmat.lz4"
        val lstmIdFile = srcdstDataDir + "data%d.imat";
        val lstmUserFile = srcdstDataDir + "data%d_usernamest.imat.lz4";

    	val sentDs = getSFDS(sentFile,nrFiles2Load)
    	val bowDs = getSFDS(bowFile,nrFiles2Load)
    	val idDs = if (dataset == 0)getFDS(idFile,nrFiles2Load) else null
    	val userDs = if (dataset == 0) getFDS(userFile,nrFiles2Load) else null

        val lstmSentDs = if (dataset == 0) getSFDS(lstmSentFile,nrFiles2Load) else null
    	val embedDs = if (dataset == 0) getFDS(embedFile,nrFiles2Load*4) else null
    	val lstmIdDs = if (dataset == 0) getFDS(lstmIdFile,nrFiles2Load) else null
    	val lstmUserDs = if (dataset == 0) getFDS(lstmUserFile,nrFiles2Load) else null      
            
        var tot = 0
        Mat.useCache = false
        while (sentDs.hasNext){
    		val sent = (sentDs.next)(0).asInstanceOf[SMat]
    		val bow = (bowDs.next)(0).asInstanceOf[SMat]
    		val id = if (dataset == 0)(idDs.next)(0).asInstanceOf[IMat] else null
    		val user = if (dataset == 0) (userDs.next)(0).asInstanceOf[IMat] else null
            tot += sent.ncols
            sent_data += sent.copy;
            bow_data += bow.copy;
            if (dataset == 0) id_data += id.copy;
            if (dataset == 0) user_data += user.copy;
        }    
        tmp_bow_data = bow_data(0).copy.asInstanceOf[SMat]
        tot = 0
        if (dataset == 0) {
            while (lstmSentDs.hasNext){
                val sent = (lstmSentDs.next)(0).asInstanceOf[SMat]
                val embed = (embedDs.next)(0).asInstanceOf[FMat]
                val id = (lstmIdDs.next)(0).asInstanceOf[IMat]
                val user = (lstmUserDs.next)(0).asInstanceOf[IMat]
                tot += user.length
                lstm_sent_data += sent.copy;
                lstm_embed_data += embed.copy;
    //            println(norm(lstm_embed_data(0)),norm(lstm_embed_data(lstm_embed_data.length-1)),norm(embed))
    //            println(lstm_embed_data(0).GUID,lstm_embed_data(lstm_embed_data.length-1).GUID,embed.GUID)
                lstm_id_data += id.copy;
                lstm_user_data += user.copy;
            }  
    /*        for(i<-0 until lstm_sent_data.length){
                println((i,lstm_sent_data(i).nnz,norm(lstm_embed_data(i).asInstanceOf[FMat])))
            }*/
            tmp_embed_data = lstm_embed_data(0).copy.asInstanceOf[FMat]
        }
        Mat.useCache = true
        println("Finished loading %d files in %.3f seconds..." format (nrFiles2Load,toc))
    }
    
//    var (dict, dataMat, sents, w2vMat, nValidSents, labels, users) = loadMemSentences_CPU(percData, corpus, seed)

    // Sizes
    // dataMat -> FMat 300 x #sentences
    // sents -> SMat 500 x #sentences
    // w2vMat -> FMat 300 x #words
    // nValidSents -> Int
    // labels -> IMat 3 x #sentences

    // var userDict = loadDict("/home/ana/userDict.sbmat", pad=false);
    // val users = loadSBMat("/home/franky/metadata_dicts/users.sbmat.lz4");


    def make_query_vec( query : String) : FMat = {

        val query_vec = FMat(size(w2vMat, 1), 1);
        println("here")
        // Converts input query to dictionary indexes
        val ss = query.toLowerCase().split(" ")

        val weights = Array.fill(ss.length+1){1.0f}; // Create a weight vector
        
        for (i <- 0 until ss.length) {
          val str = ss(i).toLowerCase();
          if (str(0) == '[' && str(str.length - 1) == ']') {
            // Convert weight inside the brackets into a double
            weights(i) = (str.stripPrefix("[").stripSuffix("]").trim).toFloat;
            ss(i) = null;
          }
        }

        println("here")
        // Convert input query to a word2vec vector
        for(i <- 0 until ss.length) {
          if(ss(i) != null) {
            val s = ss(i).toLowerCase();

            if(dict(s) == -1) {
              printf("WARNING: did not find %s in master dict\n", s);
            } else {
              val vec = w2vMat(?, dict(s));

              if(sum(vec^2)(0) == 0) {
                // printf("WARNING: %s is not in google wordvec database\n", s);
              } else {
                // printf("adding %s to vector\n", s);
                query_vec ~ query_vec + (vec * weights(i+1));
              }
            }
          }
        }
        println("here")
      // Normalize
      // size 300x1
      query_vec ~ query_vec / norm(query_vec);

      return(query_vec);
    }
    
    def query( query_s : String , top : Int, filter: String = "", minWords: Int = 5, maxWords : Int = 100,embedding: Int = 1) = {
      println(query_s)
      // Convert input query to a word2vec vector
      tic
      val query_vec = if (embedding == 0) make_query_vec(query_s) else lstmEmbedObj.genVec(query_s)
      println("Finish making vec in %.3f seconds" format toc)
      tic

      var filterRegex = new Regex("");
      if(filter=="" || filter == null) {
        filterRegex = null;
      } else {
        filterRegex = new Regex(filter);
      }

      // Compute the score of each sentence
      // Note that due to normalization, dataMat has NaNs
      // Need to filter res==NaN
      var count = 0;
      val topPerFile = if (top <= 20) top else 20
      val scores = ListBuffer[Float]();
      val ressents = ListBuffer[String]();
      val moods = ListBuffer[String]();
      val urls = ListBuffer[String]();
      val sdata = if (embedding == 0) sent_data else lstm_sent_data  
      val udata = if (embedding == 0) user_data else lstm_user_data 
      val idata = if (embedding == 0) id_data else lstm_id_data  
      var computeTime = 0f
      for(k<-0 until sdata.length){
//          println(k)
          tic
          val dataMat = if (embedding == 0){
//              bow_data(k).copyTo(tmp_bow_data)
              val magic = w2vMat* bow_data(k)
//              val magic = w2vGMat* GSMat(tmp_bow_data)
              magic ~ magic / (sqrt(magic dot magic)+1e-7f)
              magic
          }
          else{
              lstm_embed_data(k).copyTo(tmp_embed_data)
              val d = GMat(tmp_embed_data)
//              println(k,tmp_embed_data.data.take(10).toList,GPUmem)
              d/sqrt(snorm(d))
          }
          val sents = sdata(k).asInstanceOf[SMat]
          val users = if (dataset == 0)udata(k).asInstanceOf[IMat] else null
          val labels = if (dataset == 0) idata(k).asInstanceOf[IMat] else null
          val res = FMat(query_vec.t * dataMat);  // 1x#sentences
          res(find(1-((res dot res)>=0))) = -1; // sentence sums to 0
          val (x, bestIndex) = sortdown2(res);
          computeTime += toc
//          println(res.length)
//          println(res.data.toList.reverse.take(10))

          var nwords = size(sents)(0);
          var prev_res = -1f;

          var userId = 0;
          var user = "";
          var url = "";

          var i = 0;
          var last = if (bestIndex.length < topPerFile) bestIndex.length else topPerFile
              
          while(i<last) {
            var ix = bestIndex(i);
//            println(x(i),ix)
            if (x(i)-1f >= -1e-6f)
                println(k,ix)
            val (_,_,s00) = find3(sents(?,ix))
            val s0 = s00.data.filter(_>0)
            val ss = if (embedding == 0 ) s0 else s0.reverse
            val sent = ss.map(x=>dict(x.toInt)).mkString(" ").replace(" ,", " ")

            var numWords = ss.length;

            if(res(ix) != prev_res // discard repeated strings, unlikely two floats are equal unless strings have same words
              && numWords >= minWords && numWords <= maxWords // min 
              && (filterRegex== null || filterRegex.findFirstIn(sent) == None) // filter for words
            ) { 
                  prev_res = res(ix);
                  if ((dataset>0)||(ix<labels.ncols && users(ix)<usernames.length && users(ix)>=0)) {
                      var mood = ""
                      if (dataset == 0) {
                          val moodid = labels(1, ix);
                          val ditemid = labels(2, ix);
                          val timestamp = labels(3, ix);
                          // val user_idx = ditemid % 100000  + 100000*(timestamp % 10000);

                          // val user = CSMat(users(?, ix))(0);
                          //println(users(ix), ix)
                          val user = (if (users(ix)>=usernames.length || users(ix)<0) {}//println("ERROR: "+users(ix));"-1"} 
                                      else usernames(users(ix)));
                          mood = moodDict(moodid);

                          url = "http://" + user + ".livejournal.com/" + ditemid + ".html";
                      }

                      // user is wrong for now, will fix later

                      //printf("%.3f -- %s -- %s -- %s \n", res(ix), sent, mood, url);
                      scores+=res(ix);
                      ressents+=sent;
                      moods+=mood;
                      urls+=url;
                      //results += "%.3f -- %s -- %s -- %s \n" format(res(ix), sent, mood, url)
                      count += 1;
                  }
              }
            // else {
            //   printf("ignoring %s\n", sent);
            // }
            i += 1;
          }
      }
      println("Compute time: %.3f, %.3f GPU mem left" format (computeTime,GPUmem._1))
//      (scores,ressents,moods,urls)
      val data = (0 until scores.length).map(i=>(scores(i),ressents(i),moods(i),urls(i))).sortBy(-_._1).take(top)
      println(data.map(x=>x._1+" "+x._2).reduce(_+"\n"+_))
      (data.map(_._1),data.map(_._2),data.map(_._3),data.map(_._4))
    }
    
    def query_adhoc( query_s : String , top : Int, filter: String = "", minWords: Int = 5, maxWords : Int = 100, embedding: Int = 0) = {
      println(query_s)
      // Convert input query to a word2vec vector
      tic
      val query_vec = if (embedding == 0) make_query_vec(query_s) else lstmEmbedObj.genVec(query_s)
      println("Finish making vec in %.3f seconds" format toc)
      tic
      var filterRegex = new Regex("");
      if(filter=="" || filter == null) {
        filterRegex = null;
      } else {
        filterRegex = new Regex(filter);
      }
        
      println("Number of Files: %d. Loading %.3f of them: %d files" format (nrDataFiles,percData,nrFiles2Load));
        
      var sentFile = if (embedding == 0) sentsDataDir + "data%d_sent.smat.lz4" else  "/data/livejournal/srcdst/src%04d.smat.lz4"
      var bowFile = sentsDataDir + "data%d.smat.lz4";
      val idFile = (if (embedding ==0 )sentsDataDir else embedDataDir) + "data%d.imat";
      val userFile = (if (embedding ==0 )sentsDataDir else embedDataDir) + "data%d_usernamest.imat.lz4";
      val embedFile = embedDataDir + "pred%04d.fmat.lz4"
      val sentDs = getSFDS(sentFile,nrFiles2Load)
      val bowDs = getSFDS(bowFile,nrFiles2Load)
      val idDs = getFDS(idFile,nrFiles2Load)
      val userDs = getFDS(userFile,nrFiles2Load)
      val embedDs = getFDS(embedFile,nrFiles2Load*4)
      
      // Compute the score of each sentence
      // Note that due to normalization, dataMat has NaNs
      // Need to filter res==NaN
      var count = 0;
      val topPerFile = if (top <= 100) top else 10
      val scores = ListBuffer[Float]();
      val ressents = ListBuffer[String]();
      val moods = ListBuffer[String]();
      val urls = ListBuffer[String]();
      var tot = 0
//      println(norm(query_vec))
      println("Processing filenames in %.3f" format toc)
      var readTime = 0f
      var computeTime = 0f
      var sortTime = 0f    
      while (sentDs.hasNext){
          tic
          val sents = (sentDs.next)(0).asInstanceOf[SMat]
          val labels = (idDs.next)(0).asInstanceOf[IMat]
          val users = (userDs.next)(0).asInstanceOf[IMat]   
          tot += users.length
//          println(tot)        
//          println(k)
          val dataMat = if (embedding == 0){
              val bow_data = (bowDs.next)(0).asInstanceOf[SMat]
              readTime += toc;tic            
//              val magic = w2vGMat* GSMat(bow_data.asInstanceOf[SMat])
              val magic = w2vMat* (bow_data.asInstanceOf[SMat])
              magic ~ magic / (sqrt(magic dot magic)+1e-7f)
              magic
          }
          else{
              val d = GMat((embedDs.next)(0).asInstanceOf[FMat])
              readTime += toc;tic            
              d/sqrt(snorm(d))
          }
          val res = FMat(query_vec.t * dataMat);  // 1x#sentences
          res(find(1-((res dot res)>=0))) = -1; // sentence sums to 0

          val (x, bestIndex) = sortdown2(res);
          computeTime += toc;tic
          //println(x)
//          println(res.length)
//          println(res.data.toList.reverse.take(10))

          var nwords = size(sents)(0);
          var prev_res = -1f;

          var userId = 0;
          var user = "";
          var url = "";

          var i = 0;
          var last = if (bestIndex.length < topPerFile) bestIndex.length else topPerFile
              
          while (i<last) {
            var ix = bestIndex(i);
            val s0 = sents(?,ix).data.filter(_>0)
            val ss = if (embedding == 0 ) s0 else s0.reverse
            val sent = ss.map(x=>dict(x.toInt)).mkString(" ").replace(" ,", " ")
//            var curr = IMat(FMat(sents(find(sents(?, ix)), ix)));
//            var z = dict(irow(ss));
//            var sent = (z ** csrow(" ")).toString().replace(" ,", " ");

            var numWords = ss.length;

            if(res(ix) != prev_res // discard repeated strings, unlikely two floats are equal unless strings have same words
              && numWords >= minWords && numWords <= maxWords // min 
              && (filterRegex== null || filterRegex.findFirstIn(sent) == None) // filter for words
            ) { 
                  prev_res = res(ix);

                  // userId = labels(0,ix);
                  // user = userDict(userId);
                  if (ix<labels.ncols && users(ix)<usernames.length && users(ix)>=0) {
                      val moodid = labels(1, ix);
                      val ditemid = labels(2, ix);
                      val timestamp = labels(3, ix);
                      // val user_idx = ditemid % 100000  + 100000*(timestamp % 10000);

                      // val user = CSMat(users(?, ix))(0);
                      //println(users(ix), ix)
                      val user = (if (users(ix)>=usernames.length || users(ix)<0) {}//println("ERROR: "+users(ix));"-1"} 
                                  else usernames(users(ix)));
                      val mood = moodDict(moodid);

                      url = "http://" + user + ".livejournal.com/" + ditemid + ".html";

                      // user is wrong for now, will fix later

    //                  printf("%.3f -- %s -- %s -- %s \n", res(ix), sent, mood, url);
                      scores+=res(ix);
                      ressents+=sent;
                      moods+=mood;
                      urls+=url;
                      //results += "%.3f -- %s -- %s -- %s \n" format(res(ix), sent, mood, url)
                      count += 1;
                  }
              }
            // else {
            //   printf("ignoring %s\n", sent);
            // }
            i += 1;
          }
          sortTime += toc
      }
//      val order = scores.zip(0 until scores.length).sortBy(-_._1).map(_._2)
      println("Loading time: %.3f" format readTime)
      println("Compute time: %.3f" format computeTime)
      println("Sort time: %.3f" format sortTime)        
      val data = (0 until scores.length).map(i=>(scores(i),ressents(i),moods(i),urls(i))).sortBy(-_._1).take(top)
      println(tot)
      (data.map(_._1),data.map(_._2),data.map(_._3),data.map(_._4))
//      (scores,ressents,moods,urls)
    }
}
    
object BufferedQuery{
    def main(args:Array[String]) {
        val q = new BufferedQuery(0.02f,"/commuter/mallickd/matrix_full_dataset/",0,1)  
//        val q = new BufferedQuery(0.002f,"/home/data/livejournal/")  
        setGPU(1)
//        q.query("I hate commute",10)
//        q.query("I feel good today",10,embedding = 0)
//        q.query("I feel good today",10,embedding = 1)
//        q.query("I hate commute",100,embedding = 1,filter = "think")
//        q.query("music is my life",100,embedding = 0)
        q.query("music",100,embedding = 0,minWords=1)
    }
}

// Example usage:
// query("cancer", 20)
// query("amazing", 10)
