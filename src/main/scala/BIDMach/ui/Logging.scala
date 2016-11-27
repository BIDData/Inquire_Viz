package BIDMach.ui

import BIDMach._
import BIDMach.models._
import BIDMat.{Mat,FMat}


object Logging {
    def loss(nn:Learner): (Model,Array[Mat])=>Array[Mat] = {
        (m:Model,data:Array[Mat]) =>{
            if (nn.reslist.length > 0)
                Array(nn.reslist(nn.reslist.length-1))
            else
                Array(FMat(0))
        }
    }
    
    def test(m:Model,data:Array[Mat]):Array[Mat] = {
        Array(FMat(1))
    }
    
    def getFuncName(func:(Model,Array[Mat])=>Array[Mat],i:Int):String = {
        val name = func.getClass.getSimpleName
        if (name.count(_=='$') == 2)
            name//.split('$')(1) 
        else
            name
    }
}