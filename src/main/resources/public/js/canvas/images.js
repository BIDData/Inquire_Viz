(function(exports)
{
    //Static function, variables here
    
    exports.Images=function(div,W,H)
    {
        var nrow=10,ncol=10
        var size=32
        var ratio=1
        var height=nrow*size*ratio, width=ncol*size*ratio
        var canvas = div.append("canvas").attr("height",height).attr("width",width)
        var ctx = canvas.node().getContext("2d")
        var image = ctx.getImageData(0,0,width,height)
        return {
            update:function(n,m,mat){
                //Assume n=100,m=32*32*3
                var len=size*size
                for(var i=0;i<nrow;i++)
                    for(var j=0;j<ncol;j++){
                        var offset=(i*ncol+j)*3*len
                        for(var x=0;x<size;x++)
                            for(var y=0;y<size;y++)
                            {
                                var off1=offset+x*size+y
                                var r=(mat[off1])
                                var g=(mat[off1+len])
                                var b=(mat[off1+len+len])
                                var off2=((i*size+x)*(ncol*size)+j*size+y)*4
                                image.data[off2]=r
                                image.data[off2+1]=g
                                image.data[off2+2]=b
                                image.data[off2+3]=255
                                //ctx.fillStyle ="rgb"+"("+r+","+g+","+b+")"
                                //ctx.fillRect(height-1-(i*size+x)*ratio,(j*size+y)*ratio,ratio,ratio);
                            }
                    }
                ctx.putImageData(image,0,0)
            }
        }
    }
})(this)