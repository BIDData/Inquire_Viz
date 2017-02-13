//Main entry
//Router to communicate with the Server

(function(exports)
 {
	 exports.main=function()
	 {
	     var ws = new WebSocket("ws://localhost:9000/socket");
	     ws.onopen=function(){
		     view.init({
                 send:function(obj){
                     ws.send(JSON.stringify(obj))
                 }
             })
		 }
	     
		 ws.onmessage=function(msg){
			 var data=JSON.parse(msg.data)
			 if (data.type=="data")
				 view.update(data)
		 }
	 }
 }
)(this)

main()
