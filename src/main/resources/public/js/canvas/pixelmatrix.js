(function(exports)
{
    //Static function, variables here
    var tot=0
    var c=d3.scale.category10(); for(var i=0;i<10;i++)c(i)
    exports.PixelMatrix=function(name,div,W,H)
    {
        var canvas = div.append("canvas").attr("height",1000).attr("width",W)
        var ctx = canvas.node().getContext("2d")
        var size = 1
        var cid=tot;tot++;
        if (name=="silhouetteGraph")cid = 4
        var color=c(cid)
        var r = parseInt(color[1]+color[2],16)
        var g = parseInt(color[3]+color[4],16)
        var b = parseInt(color[5]+color[6],16)
        return {
            update:function(n,m,mat){
                var image = ctx.getImageData(0,0,m*size,n*size)
                for(var i=0;i<n;i++)
                    for(var j=0;j<m;j++)
                    {
                        for(var di=0;di<size;di++)
                            for(var dj=0;dj<size;dj++)
                            {
                                var off=((i*size+di)*m*size+j*size+dj)*4;
                                image.data[off]=r
                                image.data[off+1]=g
                                image.data[off+2]=b
                                image.data[off+3]=mat[i*m+j]
                            }
                    }
                ctx.putImageData(image,0,0)
            }
        }
        
    }
})(this)