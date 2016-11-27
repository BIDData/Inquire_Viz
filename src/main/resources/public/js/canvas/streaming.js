(function(exports)
{
    //Static function, variables here
    var total = 0

    exports.Streaming=function(div,W,H,names)
    {
        //Construction/private function and private variables here
        //name = name || "data"
        var //margin = {top: 10, right: 10, bottom: 20, left: 50},
            margin = {top: 10, right: 10, bottom: 10, left: 10},
            width = W - margin.left - margin.right,
            height = H - margin.top - margin.bottom;
        var chart = null
        var minValue = 1000000000
        
        function init(lower_bound){
            var d1 = names.map(function(d){
                return [d]
            })
            var inf = -10000
            while (Math.floor(lower_bound-Math.abs(lower_bound)-10)<inf)
                inf*=10
    		for(var i=0;i<100;i++){
    			//d1.push()
    			d1.forEach(function(d){d.push(inf)})
    		}
    		var axes = {}
    		names.forEach(function(name){
    		    axes[name] ="y2"
    		})
    		chart = c3.generate({
    		    bindto: div,
    			size:{
    				width: width,
    				height:height
    			},
    		    data: {
    		        columns: d1,
        			axes: axes
    		    },
    			point: {
     				show: false
    			},
    			axis: {
            	    y: {
    				    show:false
    	    	    },
    			    y2: {
            		    show: true,
            		    tick:{
            		        format: function(d){return d3.round(d,5)}
            		    }
    				},
    			    x: {
                        tick: {
                    		count:5,
                            format: function (x) { return (x-99)*1; }
                        }
                    }
    			},
    			transition: {
     				 duration: 0
    			},
    			tooltip:{
    			    format:{
    			        value: function (value, ratio, id) {
    			            return (value===inf)? "-INF" : d3.round(value,5)
                        }
    			    }
    			}
    			
    		});
    		if (names.length > 5)
	            chart.toggle(names)
        }

        return {
            //Public function and variables here
            tick:function(ds) 
            {
              // push a new data point onto the back
                var d = d3.min(ds)
                if (chart==null)
                    init(d)
                if (d<minValue) {
                    minValue = Math.floor(d)
                    chart.axis.min({y:minValue,y2:minValue})
                }
                chart.flow({
    				columns: names.map(function(d,i){return [d,ds[i]]}),
    			   	length: 1,
    			  	duration: 0 
		        })
            }
        }
    }
    
})(this)
