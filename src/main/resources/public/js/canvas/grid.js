(function(exports)
{
    //Static function, variables here
    var total = 0
    
    function createStream(r,c){
        height=420
        width=520
        var stream=null
        var div=d3.select("body").append("div").attr("title","Grid "+r+"-"+c)
        $(div.node()).dialog({
                width: width+60,
                height: height+75
            }).css("overflow","auto")   
        return {
            update:function(data)
            {
                var names = []
                var values = []
                console.log(data.row)
                for(var i=0;i<data.row.length;i++)
                    if (parseInt(data.row[i])==r && parseInt(data.col[i])==c){
                        names.push(data.names[i])
                        values.push(data.values[i])
                    }
                console.log(names)
                if (names.length===0)
                    return;
                if (stream===null) {
                    stream=Streaming(div,width+30,height+0,names)//data.names.slice(0,3))
                    var res=[]
                    for(var i=0;i<data.row.length;i++)
                        res.push(data.row[i]+" "+data.col[i])
                }
                //stream.tick(data.values.slice(0,3))
                stream.tick(values)
            }
        }
    }
    
    function genGrid(n,m)
    {
        var data=[]
		for(var i=0;i<n;i++)
			for(var j=0;j<m;j++)
				data.push({i:i,j:j})
		return data
	}

    exports.Grid=function(div,W,H,row,col)
    {
        var streams=[]
        var used=[]
        var svg=div.append("svg").attr("height",H).attr("width",W)
        row=row||5
        col=col||64
        var names=null
        var values=[],time=[]
        var currentTime = 0
        function create(r,c){
            if (used[r]===undefined)used[r]=[]
            if (used[r][c])return
            used[r][c]=true
            streams.push(createStream(r,c))
        }
        
        function getContent(r,c){
            var res="Grid"+r+"-"+c
            if (names===null)return res 
            else{
                if (names[r]===undefined)return res
                if (names[r][c]===undefined)return res
                return res+"\n"+names[r][c].join("\n")
            }
        }
        
        grid = genGrid(row,col)
        svg.selectAll("element").data(grid).enter()
            .append("rect")//.append("circle")
            //.style("fill","pink")
            .attr("style","fill:grey;stroke:#e377c2;stroke-width:1;fill-opacity:0.5")
            .attr("x",function(d){return 20+d.j*10})
            .attr("y",function(d){return 20+d.i*10})
            //.attr("r",4)
            .attr("height",10)
            .attr("width",10)
            .attr("id",function(d){
                return "network"+d.i+"-"+d.j
            })
            .attr("title","title")
            .on("mouseover",function(d){
                d3.select(this).style("fill","yellow")
            })
            .on("mouseout",function(d){
                d3.select(this)//.style("fill","#f7b6d2")
                    .style("fill",function(d){if (values[d.i]===undefined ||(time[d.i][d.j]||0)<currentTime) return "grey";else return "#f7b6d2"})

            })
            .on("click",function(d){
                create(d.i,d.j)
            })
        return {
            //Public function and variables here
            update:function(data){
                //if (names==null && data.row.length>0){
                if (data.row.length>0){
                    names=[]
                    values=[]
                    time=[]
                    var mv=0;
                    currentTime = d3.max(data.logTime)
                    for (var i=0;i<data.row.length;i++){
                        var r = parseInt(data.row[i])
                        var c = parseInt(data.col[i])
                        if (names[r]===undefined){names[r]=[];values[r]=[];time[r]=[]}
                        if (names[r][c]===undefined){names[r][c]=[];values[r][c]=0;}
                        names[r][c].push(data.names[i])
                        values[r][c]=d3.max([values[r][c],parseFloat(data.values[i])])
                        time[r][c]=data.logTime[i]
                        if (time[r][c]==currentTime)
                            mv=d3.max([values[r][c],mv])
                    }
                    console.log(time)
                    svg.selectAll("rect")
                        .style("fill-opacity",function(d){if (values[d.i]===undefined ||(time[d.i][d.j]||0)<currentTime) return 0.5;else return (values[d.i][d.j]||0)/mv})
                        .style("fill",function(d){if (values[d.i]===undefined ||(time[d.i][d.j]||0)<currentTime) return "grey";else return "#f7b6d2"})
                        .each(function(d){
                            $(d3.select(this)).tooltip({
                                content: getContent(d.i,d.j),
                                show:0,
                                hide:0
                            })
                        })
                }
                
                for(var i=0;i<streams.length;i++){
                    streams[i].update(data)
                }
            }
        }
        
    }
})(this)
