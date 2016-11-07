(function(exports)
{
    //Static function, variables here
    exports.debug={}
    
    exports.TopicMatrix=function(div,width,height,info)
    {
        var svg=div.append("svg").attr("height",height).attr("width",width)
        var idX=null
        var idY=null
        var reorder=true
        var order=-1
        var tot_=0
        var maxV
        
        var topicN=20,wordN=20
        var flag=false
        var paddingX=100,paddingY=85,RX=30,RY=24
        var mat=genGrid(wordN,topicN)
        var data
        var words=info.dict
        var maxV
        var sample=30
        var order=-1
        var tot_=0            
        
        var c=d3.scale.category10()
        for(var i=0;i<10;i++) c(i)
        
        var m={}
        
        m.update=function(data)
        {
            var list = d3.range(20).map(function(i){
                var v=data.data.map(function(d){
                    return d[i]
                }).sort(function(a,b){return a-b})
                return d3.sum(v.slice(v.length-5))
            })
            exports.debug.data=list
            exports.debug.d=data
            var d=[]
            /*maxV=0
            for(var i=0;i<data.data.length;i++)
                for(var j=0;j<data.data[0].length;j++){
                    d.push({i:i,j:j,v:data.data[i][j]})
                    if (data.data[i][j]>maxV)
                        maxV=data.data[i][j]
                }*/
            do_old_update({mat:data.data,index:data.index})
            /*
            svg.selectAll("circle").remove()
            svg.selectAll("circle").data(d).enter()
                .append("circle")
                .attr("cx",function(d){return d.j*40+50})
                .attr("cy",function(d){return d.i*40+50})
                .attr("r",function(d){return d.v/maxV*15})
                .attr("fill","purple")
                .attr("opacity",0.5)*/
        }
        
        function do_old_update(data_){
            data=data_
            if (!idX) idX=d3.range(data.mat[0].length)//d3.shuffle(d3.range(data.mat[0].length))
    		 if (!idY||idY.length!=data.mat.length){
                 reorder=true
                 idY=d3.range(data.mat.length)
			 }
			 maxV=d3.max(data.mat.map(function(d){return d3.max(d)}))
             //console.log(maxV)
			 update_()
        }
        
        function genGrid(n,m)
         {
    		 var data=[]
    		 for(var i=0;i<n;i++)
    			 for(var j=0;j<m;j++)
    				 data.push({i:i,j:j})
    		 return data
    	 }
        
        
         function getData(i,j)
    	 {
			 return Math.sqrt(data.mat[idY[i]][j]/maxV)*18
		 }
		 
		 function brush(i,j)
		 {
		     info.ws.send({
                type:"adjust",
                name:"nsampv",
                topic:j,
                word:data.index[idY[i]],
                value:0.05
            })
		 }

		 function update_()
		 {
             if (reorder)
                 if (order==-1)
                 {
                     var sum=d3.range(idY.length).map(function(i)
            									  {
    												  var s=0
    												  for(var j=0;j<idX.length;j++)
    													  s+=data.mat[i][j]
    												  return s
    											  })
    			     idY.sort(function(x,y){return sum[y]-sum[x]})
                 }
                 else
                    idY.sort(function(x,y){return data.mat[y][order]-data.mat[x][order]})
             tot_++
             if (tot_>10)
                reorder=false
			 if (!flag)
			 {
				 flag=true
				 svg.selectAll("circle").data(mat).enter().append("circle").attr("opacity",0.8)
					 .attr("cx",function(d){return d.j*RX+paddingX}).attr("cy",function(d){return d.i*RY+paddingY}).attr("r",0).attr("fill",c(6))
					 .on("click",function(d){brush(d.i,d.j)})
                     .transition().duration(50).attr("r",function(d){return getData(d.i,d.j)})
                     //.on("click",function(d){brush(d.i,d.j)})
                 //console.log(topicN)
				 svg.selectAll("topic").data(d3.range(topicN)).enter().append("text")
					 //.attr("x",function(d){return paddingX+R*d}).attr("y",paddingY-30)
					 .attr("transform",function(d){return "translate("+(paddingX+RX*d)+","+(paddingY-30)+") rotate(-60)"})
					 .attr("fill",c(4)).attr("opcacity",0.7).attr("font-size","13px")
					 .text(function(d){return "Topic "+(d+1)}).attr("id",function(d){return "topic"+d})
					 .attr("class","topicName")
					 .on("mouseover",function(d)
						 {
							 order=d
                             reorder=true
							 update_()
						 })
					 .on("mouseout",function(d)
						 {
							 order=-1
                             reorder=true
							 update_()
						 })
                     /*.on("dblclick",function(d){
                         ws.send("Paras "+runId+" "+0+" "+d)
                     })*/
                         
				 svg.selectAll("words").data(d3.range(wordN)).enter().append("text")
					 .attr("transform",function(d){return "translate("+(paddingX-100)+","+(paddingY+d*RY)+")"})
					 .attr("fill",c(8)).attr("opcacity",0.7).attr("font-size","13px")
					 .text(function(d){return words[data.index[idY[d]]]}).attr("id",function(d){return "word"+d}).attr("class","words")
			 }
			 else
			 {
				 svg.selectAll("circle").data(mat)//.transition().duration(50)
				 .attr("cx",function(d){return d.j*RX+paddingX}).attr("cy",function(d){return d.i*RY+paddingY})
				 .attr("r",function(d){return getData(d.i,d.j)}).attr("fill",c(6))
                 
				 svg.selectAll(".words").transition().duration(50)
					 .text(function(d){
                         return words[data.index[idY[d]]]
                     })

			 }
		 }
        return m 
    } 
    
    
})(this)
    