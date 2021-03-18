// Off page connector
(function() {

    //ActorShapeDMP
    function ActorShapeDMP() {
        mxActor.call(this);
    };

    mxUtils.extend(ActorShapeDMP, mxActor);

    ActorShapeDMP.prototype.size = 3 / 8;

    ActorShapeDMP.prototype.redrawPath = function (c, x, y, w, h) {
        var s = h * Math.max(0, Math.min(1, parseFloat(mxUtils.getValue(this.style, 'size', this.size))));
        var arcSize = mxUtils.getValue(this.style, mxConstants.STYLE_ARCSIZE, mxConstants.LINE_ARCSIZE) / 2;
        this.addPoints(c, [new mxPoint(0, h), new mxPoint(w, h), new mxPoint(w, s), new mxPoint(w / 2, 0),
            new mxPoint(0, s)], this.isRounded, arcSize, true);
        c.end();
    };

    mxCellRenderer.prototype.defaultShapes['actorDMP'] = ActorShapeDMP;


    //ProcessShapeDMP
    function DataShapeDMP()
    {
        mxRectangleShape.call(this);
    };
    mxUtils.extend(DataShapeDMP, mxRectangleShape);
    DataShapeDMP.prototype.size = 0.1;
    DataShapeDMP.prototype.isHtmlAllowed = function()
    {
        return false;
    };
    DataShapeDMP.prototype.getLabelBounds = function(rect)
    {
        if (mxUtils.getValue(this.state.style, mxConstants.STYLE_HORIZONTAL, true) ==
            (this.direction == null ||
                this.direction == mxConstants.DIRECTION_EAST ||
                this.direction == mxConstants.DIRECTION_WEST))
        {
            var w = rect.width;
            var h = rect.height;
            var r = new mxRectangle(rect.x, rect.y, w, h);

            var inset = w * Math.max(0, Math.min(1, parseFloat(mxUtils.getValue(this.style, 'size', this.size))));

            if (this.isRounded)
            {
                var f = mxUtils.getValue(this.style, mxConstants.STYLE_ARCSIZE,
                    mxConstants.RECTANGLE_ROUNDING_FACTOR * 100) / 100;
                inset = Math.max(inset, Math.min(w * f, h * f));
            }

            r.x += inset;
            // r.width -= 2 * inset;
            // r.width += 30;
            r.width -= inset;

            return r;
        }

        return rect;
    };
    DataShapeDMP.prototype.paintForeground = function(c, x, y, w, h)
    {
        // var inset = w * Math.max(0, Math.min(1, parseFloat(mxUtils.getValue(this.style, 'size', this.size))));

        // if (this.isRounded)
        // {
        // 	var f = mxUtils.getValue(this.style, mxConstants.STYLE_ARCSIZE,
        // 		mxConstants.RECTANGLE_ROUNDING_FACTOR * 100) / 100;
        // 	inset = Math.max(inset, Math.min(w * f, h * f));
        // }

        var inset=30;
        if (w > inset) {
            c.begin();
            c.moveTo(x + inset, y);
            c.lineTo(x + inset, y + h);
            // c.moveTo(x + w - inset, y);
            // c.lineTo(x + w - inset, y + h);
            c.end();
            c.stroke();
        }
        mxRectangleShape.prototype.paintForeground.apply(this, arguments);
    };

    mxCellRenderer.prototype.defaultShapes['dataDMP'] = DataShapeDMP;


    //DataShapeDMP
    function ProcessShapeDMP()
    {
        mxActor.call(this);
    };
    mxUtils.extend(ProcessShapeDMP, mxActor);
    ProcessShapeDMP.prototype.size = 0.1;
    ProcessShapeDMP.prototype.redrawPath = function(c, x, y, w, h)
    {

        var dx = Math.min(w, h / 2);
        c.moveTo(dx, 0);
        c.lineTo(w - dx, 0);
        c.quadTo(w, 0, w, h / 2);
        c.quadTo(w, h, w - dx, h);
        c.lineTo(dx, h);
        c.quadTo(0, h, 0, h / 2);
        c.quadTo(0, 0, dx, 0);
        c.close();
        c.end();

    };

    mxCellRenderer.prototype.defaultShapes['processDMP'] = ProcessShapeDMP;

})();