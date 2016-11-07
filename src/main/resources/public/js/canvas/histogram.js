(function(exports)
{
    //Static function, variables here
    
    exports.Histogram=function(div,W,H)
    {
        // Generate a Bates distribution of 10 random variables.
        var svg=div.append("svg").attr("height",H).attr("width",W)
        
        function plotText(data)
        {
            svg.selectAll("text").remove()
                svg.selectAll("text")
                    .data(data.map(function(d){return parseInt(d)}))
                    .enter()
                    .append("text").attr("fill","orange")
                    .attr("font-size","13px")
                    .text(function(d){return d})
                    .attr("transform",function(d,i){return "translate("+(50)+","+(i*20)+")"})   
        }
        var tot=0;
        var mi=1000000000,ma=-100
        
        return {
            update:function(datum)
            {
                
                var values = datum//.map(function(d){return parseInt(d)})
                /*for(var i=0;i<values.length;i++)
                    if (values[i]<10)
                        values[i] = 10*/
                if (tot>=0){
                    if (d3.min(values)<mi)
                        mi=d3.min(values)
                    if (d3.max(values)>ma)
                        ma=d3.max(values)
                }
                console.log(mi,ma)
                if (mi>=0&& ma>1){
                    mi=0,ma=2000
                }
                else
                {
                    mi=-1,ma=1
                }
                 
                // A formatter for counts.
                var formatCount = d3.format(",.0f");
                
                var margin = {top: 10, right: 30, bottom: 30, left: 30},
                    width = W - margin.left - margin.right,
                    height = H - margin.top - margin.bottom;
                
                var x = d3.scale.linear()
                    .domain([mi,ma])
                    .range([0, width]);
                    
                //var 
                
                // Generate a histogram using twenty uniformly-spaced bins.
                var data = d3.layout.histogram()
                    .bins(x.ticks(40))
                    (values);
                //console.log(data)
                    
                //console.log(data[0].y)
                var y = d3.scale.linear()
                    .domain([0, d3.max(data, function(d) { return d.y; })])
                    .range([height, 0]);
                //data.forEach(function(d){console.log(d.x+" "+d.y);console.log(x(d.x)+" "+y(d.y))})
                
                var xAxis = d3.svg.axis()
                    .scale(x)
                    .orient("bottom");
                
                svg.selectAll("g").remove()
                var g=svg.append("g")
                    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
                
                var bar = g.selectAll(".bar")
                    .data(data)
                  .enter().append("g")
                    .attr("class", "hisbar")
                    .attr("transform", function(d) { return "translate(" + x(d.x) + "," + y(d.y) + ")"; });
                //console.log(data[0].dx)
                //console.log(x(data[0].dx)-1)
                var w=x(data[0].dx)-x(0)
                bar.append("rect")
                    .attr("x", 1)
                    .attr("width", w - 1)
                    .attr("height", function(d) { return height - y(d.y); });
                
                bar.append("text")
                    .attr("dy", ".75em")
                    .attr("font-size","7px")
                    .attr("y", 6)
                    .attr("x", w / 2)
                    .attr("text-anchor", "middle")
                    .text(function(d) { return formatCount(d.y); });
                
                g.append("g")
                    .attr("class", "x hisaxis")
                    .attr("transform", "translate(0," + height + ")")
                    .call(xAxis)
                    .selectAll("text").attr("font-size","7px")
            }
        }   
    }
})(this)
