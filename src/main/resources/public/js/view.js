view=(function()
    {
        var v={}
        v.init=function(ws)
        {
            d3.json("assets/config.json",function(error,config){
                console.log(error,config)
                v.panel=Panel(ws,config)
            })
        }
        return v
    })()

