(function(exports)
{
    
    exports.Clusters=function(div,width,height){
        var names
        var clusters = null
        var divs = []
        var titles = []
        var num = 25
        var clusters_ = null
        var path = "/assets/data/img8/"
        
        function paint(d){
            if (clusters_ == null)return
            var id = d
            //if (d>= num/2)
            //    id +=5
            var c = d3.shuffle(clusters_[id])
            div.select("#d"+d).selectAll("img").attr("src",function(d){ return path+names[c[d]]})
            div.select("#title"+d).html("Clustering#"+id+": "+c.length)
        }
        
        function init(d){
            //var button=div.append("button").html("Refresh")
            //$(button).button().click(function(){console.log("!");div.selectAll("div").each(paint)})
            //div.on("click",function(){console.log("!");div.selectAll("div").each(paint)})
            names = d.split("\n").filter(function(d){return d.length>0})
            for(var i=0;i<num;i++){
            //div.selectAll("div").data(d3.range(num)).enter()
                titles[i] = div.append("tspan").html("Clustering#"+i+": Some random images. Waiting clustering results...").attr("id","title"+i)
                        .on("click",function(){clusters_ = clusters;div.selectAll("div").each(paint)})
                divs[i]=div.append("div")
                    .attr("id","d"+i)
                    .on("click",paint)
                    .data([i])
                divs[i].selectAll("img").data(d3.range(20))
                    .enter().append("img")
                    .attr("src",function(d){return path+names[parseInt(Math.random()*names.length)]})
                    .attr("style","width:50px;height:50px")
            }
        }
        
        d3.text("/assets/data/imgfiles8.txt",init)    
        return {
            update:function(cid){
                console.log(cid.length)
                clusters = []
                for(var i=0;i<=d3.max(cid);i++)
                    clusters[i]=[]
                for(var i=0;i<cid.length;i++){
                    if (clusters[cid[i]] == undefined) console.log(cid[i])
    	            clusters[cid[i]].push(i)  
	            }
	            clusters.sort(function(x,y){return y.length-x.length})
	            if (clusters_ == null) {
	                clusters_ = clusters;
	                div.selectAll("div").each(paint)
	            }
            }
        }
    }

})(this)