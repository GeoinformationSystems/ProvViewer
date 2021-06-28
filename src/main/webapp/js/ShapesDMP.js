// Off page connector
(function () {

    //ActorShapeDMP
    function ActorShapeDMP() {
        mxActor.call(this);
    };
    mxUtils.extend(ActorShapeDMP, mxActor);
    ActorShapeDMP.prototype.size = 0.1;
    ActorShapeDMP.prototype.redrawPath = function (c, x, y, w, h) {
        var s = h * Math.max(0, Math.min(1, parseFloat(mxUtils.getValue(this.style, 'size', this.size))));
        var arcSize = mxUtils.getValue(this.style, mxConstants.STYLE_ARCSIZE, mxConstants.LINE_ARCSIZE) / 2;
        this.addPoints(c, [new mxPoint(0, h), new mxPoint(w, h), new mxPoint(w, s), new mxPoint(w / 2, 0),
        new mxPoint(0, s)], this.isRounded, arcSize, true);
        c.end();
    };
    mxCellRenderer.prototype.defaultShapes['actorDMP'] = ActorShapeDMP;


    //DataShapeDMP
    function DataShapeDMP() {
        mxRectangleShape.call(this);
    };
    mxUtils.extend(DataShapeDMP, mxRectangleShape);
    DataShapeDMP.prototype.size = 0.1;
    DataShapeDMP.prototype.isHtmlAllowed = function () {
        return false;
    };
    DataShapeDMP.prototype.paintForeground = function (c, x, y, w, h) {
        mxRectangleShape.prototype.paintForeground.apply(this, arguments);
    };
    mxCellRenderer.prototype.defaultShapes['dataDMP'] = DataShapeDMP;


    //ProcessShapeDMP
    function ProcessShapeDMP() {
        mxActor.call(this);
    };
    mxUtils.extend(ProcessShapeDMP, mxActor);
    ProcessShapeDMP.prototype.size = 0.1;
    ProcessShapeDMP.prototype.redrawPath = function (c, x, y, w, h) {
        var dx = Math.min(w, h / 2);
        c.moveTo(dx, 0);
        c.lineTo(w - dx, 0);
        c.lineTo(w, h / 2);
        c.lineTo(w - dx, h,);
        c.lineTo(dx, h);
        c.lineTo(0, h / 2);
        c.close();
        c.end();
    };
    mxCellRenderer.prototype.defaultShapes['processDMP'] = ProcessShapeDMP;

})();