function createUserHist(urls) {
    var usernames = urls.map(function(str){return str.substring(7, str.length).split(".")[0]});
    var initial_data = {}; //maps username to frequency
    var data = [];
    var unique_users = [];
    
    var i;
    
    for (i = 0; i < usernames.length; i++) {
        var username = usernames[i];
        if (!initial_data[username]) {
            initial_data[username] = 0;
            unique_users.push(username);
        }
        initial_data[username]++;
    }
    
    var j;
    for (j = 0; j < unique_users.length; j++) {
        var username = unique_users[j];
        var entry = {
            "Freq": initial_data[username],
            "Letter": username,
            "link": "http://" + username + ".livejournal.com"
        }
        data.push(entry);
    }
    
    
    
    var margin = {top: 20, right: 20, bottom: 70, left: 40},
        width = 600 - margin.left - margin.right,
        height = 300 - margin.top - margin.bottom;


    // set the ranges
    var x = d3.scale.ordinal().rangeRoundBands([0, width], .05);

    var y = d3.scale.linear().range([height, 0]);

    // define the axis
    var xAxis = d3.svg.axis()
        .scale(x)
        .orient("bottom")


    var yAxis = d3.svg.axis()
        .scale(y)
        .orient("left")
        .ticks(10);

    d3.select("#userContributions").selectAll("svg").remove();
    // add the SVG element
    var svg = d3.select("#userContributions").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
        .attr("transform", 
              "translate(" + margin.left + "," + margin.top + ")");
    
    // load the data
    d3.json(data, function(error, data) {

        data.forEach(function(d) {
            d.Letter = d.Letter;
            d.Freq = +d.Freq;
            d.link = d.link
        });

      // scale the range of the data
      x.domain(data.map(function(d) { return d.Letter; }));
      y.domain([0, d3.max(data, function(d) { return d.Freq; })]);

      // add axis
      svg.append("g")
          .attr("class", "x axis")
          .attr("transform", "translate(0," + height + ")")
          .call(xAxis)
        .selectAll("text")
          .style("text-anchor", "end")
          .attr("dx", "-.8em")
          .attr("dy", "-.55em")
          .attr("transform", "rotate(-90)" );

      svg.append("g")
          .attr("class", "y axis")
          .call(yAxis)
        .append("text")
          .attr("transform", "rotate(-90)")
          .attr("y", 5)
          .attr("dy", ".71em")
          .style("text-anchor", "end")
          .text("Counts");


      // Add bar chart
      var bars = svg.selectAll("bar")
          .data(data)
        .enter().append("rect")
          .attr("class", "bar")
          .attr("x", function(d) { return x(d.Letter); })
          .attr("width", x.rangeBand())
          .attr("y", function(d) { return y(d.Freq); })
          .attr("height", function(d) { return height - y(d.Freq); });


      bars.on("click", function(d) {
        window.open(d.link);
      });

    });
}