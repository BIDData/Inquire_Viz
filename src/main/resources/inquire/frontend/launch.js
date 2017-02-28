query_history = []
min_history = []
max_history = []
n_history = []
filter_history = []

function openTab(evt, id) {
  // Every time you open a new tab from the menu (e.g. Node Graph or queries this is called)
  var i, x, tablinks;
  x = document.getElementsByClassName("city");
  for (i = 0; i < x.length; i++) {
     x[i].style.display = "none";
  }
  tablinks = document.getElementsByClassName("tablink");
  for (i = 0; i < x.length; i++) {
      tablinks[i].className = tablinks[i].className.replace(" w3-red", "");
  }
  if (id == "NodeGraph"){
      $(".historyContainer").hide()
  }else{
      $(".historyContainer").show()
  }
  document.getElementById(id).style.display = "block";
  document.getElementById(id + 'Button').className += " w3-red";
}


function showResults(evt) {
   // function called to make the query to the scala backend and show results
   var query = d3.select("input").node().value
   var minWords = d3.select("#minWords").node().value
   var maxWords = d3.select("#maxWords").node().value
   var filterWords = d3.select("#filterWords").node().value
   var topN = d3.select("#topn").node().value
   renderResults(query, minWords, maxWords, filterWords, topN)
}

function repeatShowResults() {
   // function called to make the query to the scala backend and show results
   console.log('clicked ' + this)
   var id = this.id
   id = parseInt(id.substring(9)) -1

   var query = query_history[id];
   var minWords = min_history[id];
   var maxWords = max_history[id];
   var filterWords = filter_history[id];
   var topN = n_history[id];
   
   document.getElementById("topn").value = topN;
   document.getElementById("minWords").value = minWords;
   document.getElementById("maxWords").value = maxWords;
   document.getElementById("filterWords").value = filterWords;
   document.getElementById("textboxId").value = query;
    
   
   renderResults(query, minWords, maxWords, filterWords, topN)
}

function renderResults(query, minWords, maxWords, filterWords, topN){
   query_history.unshift(query)
   min_history.unshift(minWords)
   max_history.unshift(maxWords)
   n_history.unshift(topN)
   if (filterWords==""){
       filter_history.unshift("No filter")
   } else {
       filter_history.unshift(filterWords)
   }
      
    
    function createTable(inputData) {

      var table = document.createElement("table");
      var r,c;
      
      for (var row in inputData[0]) {
        
        r = table.insertRow(row);
        
        c_text = r.insertCell(0);
        c_text.innerHTML = '<div id="text-row-' + row + '" class="text-row">' + inputData[0][row] + '</div>';
        
        
        c = r.insertCell(1);
        c.innerHTML = inputData[1][row];
          
        c = r.insertCell(2);
        c.innerHTML = inputData[2][row];
          
        c = r.insertCell(3);
        c.innerHTML = inputData[3][row];
          
        c = r.insertCell(4);
        c.innerHTML = inputData[4][row];
      }
      return table.innerHTML;

    }
    
    
    
    query_history.unshift("Query");
    min_history.unshift("Min words");
    max_history.unshift("Max words");
    n_history.unshift("# of results");
    filter_history.unshift("Filter");
    
    var data = [
      query_history, min_history, max_history, n_history, filter_history
    ];
    
    document.getElementById("history").innerHTML = createTable(data);
    q_cells = document.getElementsByClassName("text-row");
    for (var i = 0; i< q_cells.length; i++){
        if(i != 0)
            q_cells[i].onclick = repeatShowResults;
    }
    
    query_history.shift();
    min_history.shift();
    max_history.shift();
    n_history.shift();
    filter_history.shift();
    
   d3.select("#Queries").attr("style","display:block")
   d3.select("#Queries").selectAll("p").remove()
   d3.select("#Queries").selectAll("br").remove()
   d3.select("#Queries").append("p").text("Loading...")
   d3.json("/query?data="+query + "&minWords=" + minWords + "&maxWords=" + maxWords + "&filter=" + filterWords + "&top=" + topN,function(res){
      d = res;
          
       // New code
       // Google charts interactive table
        google.charts.load('current', {'packages':['table']});
              google.charts.setOnLoadCallback(drawTable);

              function drawTable() {
                var data = new google.visualization.DataTable();
                 
                data.addColumn('number', 'Similarity');
                data.addColumn('string', 'Results');
                data.addColumn('string', 'Emotion');
                data.addColumn('string', 'User');
                data.setColumnProperty(3, {allowHtml: true});
                
                s = d.data.split("\n")
                emotions = d.emotion[0]
                similarity = d.cosine_similarity
                links = d.url[0]
                usernames = links.map(function(str){return str.substring(7, str.length).split(".")[0]});
                var usernameLinks = [];
                for (var i = 0; i < usernames.length; i++) {
                    usernameLinks.push("<a href='"+links[i]+"'target='_blank'>"+usernames[i]+"</a>");
                }
                console.log(usernames)
                console.log(usernameLinks)
                  
                data.addRows(
                  zip([ similarity,s, emotions, usernameLinks])
                );

                var table = new google.visualization.Table(document.getElementById('table_div'));

                table.draw(data, {showRowNumber: true, width: '100%', height: '100%', allowHtml: true});
              }
       
      
      
      createNodeGraph(d["query_results"][0]);
       });
}



// helper function
function zip(arrays) {
    return arrays[0].map(function(_,i){
        return arrays.map(function(array){return array[i]})
    });
}