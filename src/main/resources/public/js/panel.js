(function(exports)
 {
     //var list=["LDA","KMeans","KMeans-Cifar","KMeans-MNIST","NMF-Cifar"]//,"NMF-MNIST"]
     
	 exports.Panel=function(ws,config)
	 {
         //var list=["LDA","KMeans-Cifar","KMeans-MNIST","NMF-Cifar"]//,"NMF-MNIST"]
         var arrayid={}
         var list = config.map(function(d,i){arrayid[d.name]=i;return d.name})
         var control=d3.select("body").append("div")
                    .attr("style","position:absolute;left:10px;top:0%;height:100%; width: 25%")
                    //.style("background","linear-gradient(#DEDAD9, #C2BFBE)")
         var value={}
         var monitors=[]
              
         createMenu(list,"ModelList","Select a Model",function( event, data ) {
                 if (data.item.label!="--")
                    var name=data.item.label
                    var dataset="nytimes"
                    if (name=="KMeans-Cifar"){
                        name="KMeans"
                        dataset="cifar"
                    }
                    if (name=="KMeans-MNIST"){
                        name="KMeans"
                        dataset="MNIST"
                    }
                    if (name=="KMeans-ImageNet"){
                        name="KMeans"
                        dataset="ImageNet"
                    }
                    if (name=="NMF-Cifar"){
                        name="NMF"
                        dataset="cifar"
                    }
                    if (name=="NMF-MNIST"){
                        name="NMF"
                        dataset="MNIST"
                    }
                    if (name=="LSTM"){
                        name="LSTM"
                        dataset="test"
                    }
                    ws.send({
                        type:"model",
                        name:name,
                        dataset:dataset,
                        useGPU:config[arrayid[data.item.label]].useGPU
                    })
             })
         
         var v={}
         
         function displayName(s)
         {
             if (s=="wsize") return "sizeWeight"
             if (s=="power") return "windowTime"
             if (s=="reg1weight") return "L1-reg"
             return s
         }
         
         v.showMeta=function(data)
         {
             //if (data.type=="model")
                {
                    //console.log(data.dict.length)
                    //console.log(data.dict.slice(0,10))
                    d3.selectAll(".OutputList").remove()
                    d3.selectAll(".OptionList").remove()
                    d3.selectAll(".slider").remove()
                    createMenu(data.output.name,"OutputList","Select a Metric",function(){
                        var id=value["OutputList"]
                        data.ws=ws
                        var c=Canvas(data.output.name[id],data.output.type[id],null,data)
                        monitors.push(c)
                        ws.send({
                            type:"register",
                            name:data.output.name[id],
                            id:monitors.length-1,
                            datatype:data.output.type[id]
                        })
                    })
                    createMenu(data.opts.name,"OptionList","Select a Parameter",function(){
                        var id=value["OptionList"]
                        var name=data.opts.name[id]
                        //var max=0,
                        //if (name=="lambda")type="Float"
                        control.append("tspan").attr("id","v_"+name).style("padding","5px")
                        var s=control.append("div").attr("class","slider")//.style("padding","5px")
                        
                        function getV()
                        {
                            var v=$(s.node()).slider("value")
                            
                            /*if (name=="nsamps"){
                                v=parseInt(Math.exp((v-50)/10)*1e4)*1e-4
                            }
                            if (name=="wsize")
                                v=parseInt(Math.exp((v-50)/10)*1e4)*1e-3        
                            if (name=="lambda")
                                v=parseInt(Math.exp((v-50)/10)*1e3)*1e-7
                            if (name=="batchSize")
                                v=parseInt(Math.exp((v-50)/10)*1e3)*/
                            var ratio = Math.log(10)*1.2
                            if (name=="rate" || name == "s1" ||name =="s2" ||name=="s3")
                                v=(v/100)
                            else
                            if (name=="power")
                                v=(v/100) 
                            else
                                v=Math.exp((v-50)/10*ratio)*data.opts.value[id]
                            //if (v<1)
                              //  v=parseInt(v*10000000+1)/10000000
                            v = Math.round(v*1000000)/1000000
                            if (name=="nsamps")
                                v=parseInt(v*10)/10
                            control.select("#v_"+name).html(displayName(name)+" : "+v)
                            return v
                        }
                        
                        $(s.node()).slider({
                              orientation: "horizontal",
                              //range: "min",
                              max: 100,
                              value: 50,
                              slide: getV,
                              change: function(){
                                        ws.send({
                                            type:"adjust",
                                            name:name,
                                            value:getV()
                                        })
                              }
                            });
                        getV()
                    })
                }
         }
         
         v.update=function(data)
         {
             //console.log(data)
             monitors[data.id].update(data)
         }
         
         function createMenu(data,name,discription,func)
         {
             if (data[data.length-1]!="--")
                data.push("--")
             var div=control.append("div").attr("class",name)
             div.append("label").attr("for",name).html(discription)
                    .attr("style","display: block;margin: 5px 5px 5px 5px;")
             var models=div.append("select").attr("id",name).attr("name",name)
                        .style("width","200px").style("height","10px").style("padding","5px")
             models.selectAll("option").data(data).enter()
                    .append("option").html(function(d){return displayName(d)})
             d3.select(models.selectAll("option")[0][data.length-1]).attr("selected","selected")
             $("#"+name).selectmenu({
                 change: function(event,data){
                    value[name]=data.item.index
                    if (func)
                        func(event,data)
                 }
             });
         }
         
        return v                               
	 }
 }
 )(this)
 
 
 
 
 
 
 
 
 
 /*control.append("input").attr("style","width:80%;left:20px;position:absolute;top:160px")
            .attr("type","range").attr("onchange","adjust_m(this.value)").attr("oninput","adjust_m2(this.value)")
    	 
         control.append("tspan").html("Mixins lambda: 1e-7").attr("id","text2").attr("style","left:0px;position:absolute;top:185px")
    	 
         control.append("input").attr("style","width:80%;left:20px;position:absolute;top:0%")
	        .attr("type","range").attr("onchange","adjust(this.value)").attr("oninput","adjust2(this.value)")
		 control.append("tspan").html("Sample size: 1").attr("id","text").attr("style","left:0px;position:absolute;top:25px")
		 
         control.append("button").attr("style","width:100px;height:30px;left:20px;position:absolute;top:50px")
	        .attr("type","button").attr("onclick","start(1)").text("Start NIPS ").attr("id","testg")
		 control.append("button").attr("style","width:100px;height:30px;left:130px;position:absolute;top:50px")
            .attr("type","button").attr("onclick","start(0)").text("Start NYTimes")
    	 control.append("button").attr("style","width:100px;height:30px;left:240px;position:absolute;top:50px")
            .attr("type","button").attr("onclick","stop()").text("Stop")
            
         control.append("button").attr("style","width:100px;height:30px;left:130px;position:absolute;top:90px")
            .attr("type","button").attr("onclick","reorder()").text("Reorder")
            
         control.append("input").attr("style","width:100px;height:30px;left:130px;position:absolute;top:120px")
            .attr("type","checkbox").attr("id","check")
         control.append("tspan").attr("style","width:200px;height:30px;left:150px;position:absolute;top:130px")
            .html("Change model directly")*/
 