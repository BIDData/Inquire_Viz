(function(exports)
{
    exports.Canvas=function(name,type,para,info)
    {
        para=para||{}
        var v={}
        var height,width
        var div=d3.select("body").append("div").attr("title",name)
        
        
        if (type=="scalar"){
            height=para.height||220
            width=para.width||320
            
            var stream=Streaming(div,width+30,height+0,[name])
            v.update=function(data)
            {
                stream.tick([data.data])
            }
        }
            
        if (type=="topicMatrix"){
            height=para.height||570
            width=para.width||700
            var mat = TopicMatrix(div,width,height,info)
            v.update = mat.update
        }
        
        if (type=="images"){
            height=para.height||400
            width=para.width||400
            var img=Images(div,width,height)
            v.update=function(data)
            {
                img.update(data.dim[0],data.dim[1],data.data)
            }
        }
        
        if (type=="histogram"){
            height=para.height||400
            width=para.width||400
            var his=Histogram(div,width,height)
            v.update=function(data){
                his.update(data.data)   
            }
        }
        
        if (type=="pixelMatrix")
        {
            height=para.height||300
            width=para.width||300
            var pixel=PixelMatrix(name,div,width,height)
            v.update=function(data)
            {
                pixel.update(data.dim[0],data.dim[1],data.data)
            }
        }
        
        if (type=="gradient"){
            height=para.height||100
            width=para.width||700
            var grid = Grid(div,width,height)
            v.update=function(data) {
                grid.update(data)
            }
        }
        
        if (type=="clusters"){
            height=para.height||300
            width=para.width||600
            //var grid = Grid(div,width,height)
            var c = Clusters(div,width,height)
            v.update=function(data) {
                c.update(data.data)
                //console.log(data)
            }
        }
             
        $(div.node()).dialog({
                width: width+60,
                height: height+75
            }).css("overflow","auto")     
        return v
    }
}
)(this)
