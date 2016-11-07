(function(exports)
{
    //Static function, variables here
    var total = 0
    exports.Streaming=function(div,W,H,len)
    {
        //Construction/private function and private variables here
        H-=70
        var n=len||100
        var up=1,down=1
        var Y=25;
        var data = d3.range(n).map(function(){return -100000/*Y-down*/});
        var gid = total; total++;
         
        var margin = {top: 10, right: 10, bottom: 20, left: 50},
            width = W - margin.left - margin.right,
            height = H - margin.top - margin.bottom;
         
        var x = d3.scale.linear()
            .domain([0, n - 1])
            .range([0, width]);
         
        var y = d3.scale.linear()
            .domain([d3.min([0,Y+down]), Y-down])
            .range([0, height]);
         
        var line = d3.svg.line()
            .x(function(d, i) { return x(i); })
            .y(function(d, i) { return y(d); });
         
        var svg = div.append("svg").attr("height",H).attr("width",W)
            //.attr("width", width + margin.left + margin.right)
            //.attr("height", height + margin.top + margin.bottom)
          .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")")
            .attr("class","svg")
         
        svg.append("defs").append("clipPath")
            .attr("id", "clip")
          .append("rect")
            .attr("width", width)
            .attr("height", height);
         
        svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + y(Y-down) + ")")
            .call(d3.svg.axis().scale(x).orient("bottom"));
         
        svg.append("g")
            .attr("class", "yaxis")
            .call(d3.svg.axis().scale(y).orient("left"));
         
        var path = svg.append("g")
            .attr("clip-path", "url(#clip)")
          .append("path")
            .datum(data)
            .attr("class", "line")
            .attr("d", line);   
        var upp=div.append("div")
        upp.append("span").html("Upper bound:")
        upp.append("input").attr("type","text").attr("id","up"+gid).attr("size","20")
        var downp=div.append("div")
        downp.append("span").html("Lower bound:")
        downp.append("input").attr("type","text").attr("id","down"+gid).attr("size","20")
        div.append("button").on("click",function(){
            //alert(d3.select("#up").attr("value"))
            //alert(document.getElementById("up").value)
            var upv=parseFloat(document.getElementById("up"+gid).value)
            var downv=parseFloat(document.getElementById("down"+gid).value)
            tot=-1
            updateY(downv,upv)
        }).html("Change")
            
        var sum=0,tot=0
        
        function updateY(downv,upv)
        {
            var y = d3.scale.linear()
                    .domain([upv, downv])
                    .range([0, height]);
            div.select("#up"+gid).attr("value",upv)
            div.select("#down"+gid).attr("value",downv)
            line.y(function(d, i) { return y(d); });
            svg.selectAll(".yaxis").remove()
            svg.append("g")
                .attr("class", "yaxis")
                .call(d3.svg.axis().scale(y).orient("left"));
        }
        
        return {
            //Public function and variables here
            tick:function(d) 
            {
              // push a new data point onto the back
              console.log(d)
              data.push(d);
              if (tot>=0){
                sum+=d;tot+=1;
                updateY(Math.round(sum/tot)-down,Math.round(sum/tot)+down)
              }
              // redraw the line, and slide it to the left
              path
                  .attr("d", line)
                  .attr("transform", null)
                .transition()
                  .duration(500)
                  .ease("linear")
                  .attr("transform", "translate(" + x(-1) + ",0)")
                  //.each("end", tick);
             
              // pop the old data point off the front
              data.shift();
            }    
        }
    }
    
})(this)
