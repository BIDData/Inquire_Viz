function createNodeGraph(query_results) {
    var svg = d3.select("#NodeGraph").select("svg"),
    width = +svg.attr("width"),
    height = +svg.attr("height");
    
    svg.selectAll("g").remove();

var color = d3.scaleOrdinal(d3.schemeCategory20);

var simulation = d3.forceSimulation()
    .force("link", d3.forceLink().id(function(d) { return d.id; }))
    .force("charge", d3.forceManyBody())
    .force("center", d3.forceCenter(width / 2, height / 2));

d3.request("/api/visualize/epsilon")
    .header("Content-Type", "application/json")
    .post(
        JSON.stringify(
            {
                results: query_results
                }
        ),
        function(error, rawData){
          var graph = JSON.parse(rawData.response);
            console.log("got response", graph);

  if (error) throw error;

 var louvainNodes = [];
 for(i in graph.nodes) louvainNodes.push(graph.nodes[i].id);
 var community = jLouvain().nodes(louvainNodes).edges(graph.links);
 var partition = community();


  var link = svg.append("g")
      .attr("class", "links")
    .selectAll("line")
    .data(graph.links)
    .enter().append("line")
      .attr("stroke-width", function(d) { return Math.sqrt(d.value); });

  console.log(partition)
  
  var node = svg.append("g")
      .attr("class", "nodes")
    .selectAll("circle")
    .data(graph.nodes)
    .enter().append("circle")
      .attr("r", 5)
      .attr("fill", function(d) { return color(partition[d.id]); })
      .call(d3.drag()
          .on("start", dragstarted)
          .on("drag", dragged)
          .on("end", dragended));

  node.append("title")
      .text(function(d) { return d.text; });
  
  node.on("mouseover", mouseover);
  node.on("mouseout", mouseout);
  var div = d3.select("body").append("div")	
    .attr("class", "tooltip")
    .style("opacity", 0);
            
  function mouseover(d) {
      div.transition()
         .duration(100)
         .style("opacity", .9);
      
      div.text(d.text)
         .style("left", (d3.event.pageX) + "px")
         .style("top", (d3.event.pageY - 28) + "px");
  };
  function mouseout(d) {
      div.transition()
         .duration(200)
         .style("opacity", 0);
  };

  simulation
      .nodes(graph.nodes)
      .on("tick", ticked);

  simulation.force("link")
      .links(graph.links);

  function ticked() {
    link
        .attr("x1", function(d) { return d.source.x; })
        .attr("y1", function(d) { return d.source.y; })
        .attr("x2", function(d) { return d.target.x; })
        .attr("y2", function(d) { return d.target.y; });

    node
        .attr("cx", function(d) { return d.x; })
        .attr("cy", function(d) { return d.y; });
  }
});

function dragstarted(d) {
  if (!d3.event.active) simulation.alphaTarget(0.3).restart();
  d.fx = d.x;
  d.fy = d.y;
}

function dragged(d) {
  d.fx = d3.event.x;
  d.fy = d3.event.y;
}

function dragended(d) {
  if (!d3.event.active) simulation.alphaTarget(0);
  d.fx = null;
  d.fy = null;
}

};