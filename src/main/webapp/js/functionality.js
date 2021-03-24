
$(document).ready(function () {
    $("#getDatasets").bind("click", function () {
        var endpointUrl = document.getElementById("endpoint").value;
            sparqlQuery = [
                "PREFIX prov: <http://www.w3.org/ns/prov#>",
                "PREFIX dct: <http://purl.org/dc/terms/>",
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>",
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
                "SELECT ?entity ?entityTitle WHERE {",
                "?entity a prov:Entity .",
                "OPTIONAL {?entity dct:title ?entityTitle .}",
                "OPTIONAL {?entity skos:label ?entityTitle .}",
                "OPTIONAL {?entity rdfs:label ?entityTitle .}",
                "}"
            ].join(" ");
        settings = {
            headers: { Accept: 'application/sparql-results+json' },
            data: { query: sparqlQuery }
        };

        $("#selectionBox").empty();
        $.ajax(endpointUrl, settings).then(function (data) {
            // $( 'body' ).append( ( $('<pre>').text( JSON.stringify( data) ) ) );

            let results = data["results"]["bindings"];
            console.log(results);
            let select = document.getElementById("selectionBox");
            for (let i = 0; i < results.length; i++) {
                option = document.createElement("option")
                option.value = (results[i]["entity"]["value"]);
                if (results[i]["entityTitle"] != null){
                    option.text = (results[i]["entityTitle"]["value"]);
                } else {
                    option.text = (results[i]["entity"]["value"]);
                }
                
                selectionBox.add(option);

            }
        });
    });
});
$(document).ready(function () {
    $("#addToCanvas2").bind("click", function () {
        console.log(document.getElementById("selectionBox").options[document.getElementById("selectionBox").selectedIndex].value)
    });
});
function onInit(editor) {
    $("#addToCanvas").bind("click", function (e) {

        $.ajax({
            type: 'POST',
            url: "./importPROV",
            data: {
                'entityName': document.getElementById("selectionBox").options[document.getElementById("selectionBox").selectedIndex].value,
                'pathLen': String(document.getElementById("provLength").value),
                'endpoint': document.getElementById("endpoint").value
            },
            success: function (data) {
                if (data == null)
                    return;

                $('#myModal').modal('hide');
                editor.graph.getModel().beginUpdate();
                console.log(data)
                try {
                    editor.graph.removeCells(editor.graph.getChildCells(editor.graph.getDefaultParent(), true, true));
                    var root = mxUtils.parseXml(data).documentElement;
                    var dec = new mxCodec(root.ownerDocument);
                    dec.decode(root, editor.graph.getModel());
                }
                finally {
                    // Updates the display and fit the graph
                    editor.graph.getModel().endUpdate();
                    var layout = new mxHierarchicalLayout(editor.graph, mxConstants.DIRECTION_WEST, true);
                    layout.interHierarchySpacing = 30;
                    layout.interRankCellSpacing = 30;
                    layout.intraCellSpacing = 30;
                    var parent = editor.graph.getDefaultParent();
                    layout.execute(parent);
                    editor.graph.fit();
                    editor.graph.view.rendering = true;
                    editor.graph.refresh();
                }
            }
        });


    });

    mxVertexHandler.prototype.rotationEnabled = false;
    mxGraphHandler.prototype.guidesEnabled = true;
    mxGuide.prototype.isEnabledForEvent = function (evt) {
        return !mxEvent.isAltDown(evt);
    };

    mxConnectionHandler.prototype.connectImage = new mxImage('images/DMP/right-arrow.png', 16, 16);

    editor.graph.setConnectable(false);
    editor.graph.setConnectableEdges(false);
    editor.graph.setAllowDanglingEdges(false);
    editor.graph.setDisconnectOnMove(false);

    editor.graph.multiplicities.push(new mxMultiplicity(true, 'DataDMP', null, null, 'n', 'n', ['DataDMP'],
        'Count Error data data',
        'Connection from Data to Data not allowed', false));
    editor.graph.multiplicities.push(new mxMultiplicity(true, 'ProcessDMP', null, null, 'n', 'n', ['ProcessDMP'],
        'Count Error process Process',
        'Connection from Process to Process not allowed', false));
    editor.graph.multiplicities.push(new mxMultiplicity(false, 'ActorDMP', null, null, 0, 0, null,
        'Actor must have NO incoming edge',
        null));

    editor.validating = true;

    var title = document.getElementById('title');

    if (title != null) {
        var f = function (sender) {
            title.innerHTML = 'mxDraw - ' + sender.getTitle();
        };

        editor.addListener(mxEvent.ROOT, f);
        f(editor);
    }

    mxEvent.addMouseWheelListener(function (evt, up) {
        if (!mxEvent.isConsumed(evt)) {
            if (up) {
                editor.execute('zoomIn');
            }
            else {
                editor.execute('zoomOut');
            }

            mxEvent.consume(evt);
        }
    });
    mxEvent.addListener(document.getElementById("plusButton"), 'click', function () {
        editor.execute('zoomIn');

    });
    mxEvent.addListener(document.getElementById("minusButton"), 'click', function () {
        editor.execute('zoomOut');
    });
    mxEvent.addListener(document.getElementById("fitButton"), 'click', function () {
        editor.execute('fit')
    });
    mxEvent.addListener(document.getElementById("hierarchicalButton"), 'click', function () {
        var layout = new mxHierarchicalLayout(editor.graph, mxConstants.DIRECTION_WEST, true);
        layout.interHierarchySpacing = 30;
        layout.interRankCellSpacing = 30;
        layout.intraCellSpacing = 30;
        var parent = editor.graph.getDefaultParent();
        layout.execute(parent);
        editor.graph.fit();
        editor.graph.view.rendering = true;
        editor.graph.refresh();
    });

    editor.graph.addListener(mxEvent.LABEL_CHANGED, function (sender, evt) {
        editor.graph.refresh();
        editor.graph.updateCellSize(evt.properties.cell);
    });

    var graphGetPreferredSizeForCell = editor.graph.getPreferredSizeForCell;
    editor.graph.getPreferredSizeForCell = function (cell) {
        var result = graphGetPreferredSizeForCell.apply(this, arguments);
        var style = this.getCellStyle(cell);

        if (style['minHeight'] > 0) {
            result.height = Math.max(style['minHeight'], result.height);
        }
        if (style['minWidth'] > 0) {
            result.width = Math.max(style['minWidth'], result.width);
            if (style['shape'] == "dataDMP") {
                result.width += 50;
            }
        }
        return result;
    };

    editor.graph.connectionHandler.addListener(mxEvent.CONNECT, function (sender, evt) {
        var edge = evt.getProperty('cell');
        var source = editor.graph.getModel().getTerminal(edge, true);

        if (source.getStyle() == "actorDMP") {

            var style = editor.graph.getCellStyle(edge);
            var newStyle = editor.graph.stylesheet.getCellStyle("edgeStyle=elbowEdgeStyle;html=1;dashed=1;rounded=1;jettySize=auto;orthogonalLoop=1;strokeColor=#727675;strokeWidth=1;endArrow=openThin;startArrow=oval;", style); //Method will merge styles into a new style object.  We must translate to string from here
            var array = [];
            for (var prop in newStyle)
                array.push(prop + "=" + newStyle[prop]);
            edge.style = array.join(';');
        }
    });



    editor.graph.flipEdge = function (edge) {
        if (edge != null) {
            var state = this.view.getState(edge);
            var style = (state != null) ? state.style : this.getCellStyle(edge);

            if (style != null) {
                var elbow = mxUtils.getValue(style, mxConstants.STYLE_ELBOW,
                    mxConstants.ELBOW_HORIZONTAL);
                var value = (elbow == mxConstants.ELBOW_HORIZONTAL) ?
                    mxConstants.ELBOW_VERTICAL : mxConstants.ELBOW_HORIZONTAL;
                this.setCellStyles(mxConstants.STYLE_ELBOW, value, [edge]);
            }
        }
    };

    editor.addAction('editMetadata', function (editor, cell) {
        console.log(editor);
        console.log(cell);

        if (cell != null) {
            var dlg = new EditDataDialog(editor, cell);
            $('#editMetadataModal').modal('show');
            // ui.showDialog(dlg.container, 320, 320, true, false);
            // dlg.init();
        }
    })
}

window.onbeforeunload = function () {
    return mxResources.get('changesLost');
};
