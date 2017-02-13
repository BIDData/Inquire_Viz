/** Controlling all the visual elements 
 * 
 **/
(function(exports){
    var v = {}
    var control=d3.select("body").append("div")
                .attr("style","position:absolute;left:10px;top:0%;height:100%; width: 25%")
    var initialValues
    var monitor = {}
    
    function displayName(s)
     {
         if (s=="wsize") return "sizeWeight"
         if (s=="power") return "windowTime"
         if (s=="reg1weight") return "L1-reg"
         return s
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
             change: function(event,d){
                if (func)
                    func(data[d.item.index],d.item.index)
             }
         });
    }
    
    function selectParameter(d,i) {
         control.append("tspan").attr("id","v_"+d).style("padding","5px")
            var s=control.append("div").attr("class","slider")//.style("padding","5px")
            
            function getV()
            {
                var v=$(s.node()).slider("value")
                var ratio = Math.log(10)*1.2
                if (name=="rate" || name == "s1" ||name =="s2" ||name=="s3")
                    v=(v/100)
                else
                if (name=="power")
                    v=(v/100) 
                else
                    v=Math.exp((v-50)/10*ratio) * initialValues
                v = Math.round(v*1000000)/1000000
                if (name=="nsamps")
                    v=parseInt(v*10)/10
                control.select("#v_"+name).html(displayName(name)+" : "+v)
                return v
            }
            
            $(s.node()).slider({
                  orientation: "horizontal",
                  max: 100,
                  value: 50,
                  slide: getV,
                  change: function(){
                  }
                });
            getV()
    }

    function selectMetric(d,i) {
        var c=Canvas(d,"scalar",null)
        monitor[d] = c
    }

    v.init=function(wsobjs)
    {
        d3.json("meta/",function(meta){
            initialValues = meta.parameters
            control.append("div").html(meta.model)
                .attr("style","display: block;margin: 5px 5px 5px 5px;")
            createMenu(Object.keys(meta.parameters),"OptionList","Select a Parameter",selectParameter)
            createMenu(meta.metrics,"MetricList","Select a Metric",selectMetric)
        })
    }
    
    v.update=function(data) {
        if (data.type == "data"){
            for(var i=0;i<data.data.length;i++)
                if (data.data[i].name in monitor)
                    monitor[data.data[i].name].update({"data":d3.mean(data.data[i].data)})
        }
    }
    
    exports.view = v
})(this)



