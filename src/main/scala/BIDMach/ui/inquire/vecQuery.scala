package BIDMach.ui.inquire

import utils._
import memQuery._
import scala.util.matching.Regex
import BIDMat.{CMat, CSMat, DMat, Dict, FMat, FND, GMat, GDMat, GIMat, GLMat, GSMat, GSDMat, HMat, IDict, Image, IMat, LMat, Mat, SMat, SBMat, SDMat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import scala.collection.mutable.ListBuffer


object VecQuery {
    Mat.checkMKL
    Mat.checkCUDA
    Mat.useCache= true

    var percData = 0.006;  // no more than 0.012
    var corpus = 0;  // select 1-for LJ; 0-for Google
    var seed = 94720;  // default is 94720
                       // Loads, converts to w2v and normalizes
    var (dict, dataMat, sents, w2vMat, nValidSents, labels, users) = loadMemSentences_CPU(percData, corpus, seed)

    // Sizes
    // dataMat -> FMat 300 x #sentences
    // sents -> SMat 500 x #sentences
    // w2vMat -> FMat 300 x #words
    // nValidSents -> Int
    // labels -> IMat 3 x #sentences

    // var userDict = loadDict("/home/ana/userDict.sbmat", pad=false);
    // val users = loadSBMat("/home/franky/metadata_dicts/users.sbmat.lz4");

    var moodDict = loadDict("/home/franky/metadata_dicts/moods_dict.sbmat.lz4", pad=false);

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
              var vec = FMat(w2vMat(?, dict(s)));

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

    def query( query_s : String , top : Int, filter: String = "", minWords: Int = 5, maxWords : Int = 100) = {

      // Convert input query to a word2vec vector
      val query_vec = make_query_vec(query_s);

      var filterRegex = new Regex("");
      if(filter=="" || filter == null) {
        filterRegex = null;
      } else {
        filterRegex = new Regex(filter);
      }

      println();

      // Compute the score of each sentence
      // Note that due to normalization, dataMat has NaNs
      // Need to filter res==NaN
      var res = query_vec.t * dataMat;  // 1x#sentences
      res(find(1-((res dot res)>=0))) = -1; // sentence sums to 0

      println("Sorting Results");
      // Sort Results to Return Top Ones
      var (x, bestIndex) = sortdown2(res);

      var nwords = size(sents)(0);
      var prev_res = -1f;

      var userId = 0;
      var user = "";
      var url = "";

      var i = 0;
      var count = 0;
      val scores = ListBuffer[Float]();
      val ressents = ListBuffer[String]();
      val moods = ListBuffer[String]();
      val urls = ListBuffer[String]();
        
      // for(i <- 0 until bestIndex.length) {
      while((count < top) && (i<bestIndex.length)) {
        var ix = bestIndex(i);
        var curr = IMat(FMat(sents(find(sents(?, ix)), ix)));
        var z = dict(curr).t;
        var sent = (z ** csrow(" ")).toString().replace(" ,", " ");

        var numWords = z.length;

        if(res(ix) != prev_res // discard repeated strings, unlikely two floats are equal unless strings have same words
          && numWords >= minWords && numWords <= maxWords // min 
          && (filterRegex== null || filterRegex.findFirstIn(sent) == None) // filter for words
        ) { 
          prev_res = res(ix);

          // userId = labels(0,ix);
          // user = userDict(userId);

          val moodid = labels(1, ix);
          val ditemid = labels(2, ix);
          val timestamp = labels(3, ix);
          // val user_idx = ditemid % 100000  + 100000*(timestamp % 10000);

          // val user = CSMat(users(?, ix))(0);
          val user = users(ix);
          val mood = moodDict(moodid);

          url = "http://" + user + ".livejournal.com/" + ditemid + ".html";

          // user is wrong for now, will fix later

          printf("%.3f -- %s -- %s -- %s \n", res(ix), sent, mood, url);
          scores+=res(ix);
          ressents+=sent;
          moods+=mood;
          urls+=url;
          //results += "%.3f -- %s -- %s -- %s \n" format(res(ix), sent, mood, url)
          count += 1;
        }
        // else {
        //   printf("ignoring %s\n", sent);
        // }
        i += 1;
      }
      println();      
      (scores,ressents,moods,urls)
    }
    
    def main(args:Array[String]) {
        query("I hate commute",10)
    }
}

// Example usage:
// query("cancer", 20)
// query("amazing", 10)
