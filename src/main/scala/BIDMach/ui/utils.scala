package BIDMach.ui

import BIDMach._
import BIDMach.models._

object utils {
    def getOptions(opts: BIDMat.Opts) = {
        val ignore = List("what", "wait", "equals", "toString", "hashCode", "getClass", "notify", "notifyAll", "ignore", "copyFrom")
        val a = for (meth <- opts.getClass.getMethods 
    		if (!meth.getName.contains("$eq") && !meth.getName.contains("$$methOrdering") && !ignore.contains(meth.getName))) yield meth;
        a.map(x=>{
            val v = x.invoke(opts)
            (x.getName,if (v == null) "null" else v.toString)   
        })
    }
    
    
}
