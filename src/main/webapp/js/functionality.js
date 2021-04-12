
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
                if (results[i]["entityTitle"] != null) {
                    option.text = (results[i]["entityTitle"]["value"]);
                } else {
                    option.text = (results[i]["entity"]["value"]);
                }

                selectionBox.add(option);

            }
        });
    });
});

function updateCanvas(editor, data) {
    editor.graph.getModel().beginUpdate();
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

function onInit(editor) {
    $("#newGraph").bind("click", function (e) {
        $.ajax({
            type: 'POST',
            url: "./importPROV",
            data: {
                'entityName': document.getElementById("selectionBox").options[document.getElementById("selectionBox").selectedIndex].value,
                'pathLen': '1',
                'endpoint': document.getElementById("endpoint").value,
                'destroySession': 'true'
            },
            success: function (data) {
                if (data == null)
                    return;

                $('#myModal').modal('hide');
                updateCanvas(editor, data)
            }
        });
    });

    $("#addToGraph").bind("click", function (e) {
        $.ajax({
            type: 'POST',
            url: "./importPROV",
            data: {
                'entityName': document.getElementById("selectionBox").options[document.getElementById("selectionBox").selectedIndex].value,
                'pathLen': '1',
                'endpoint': document.getElementById("endpoint").value,
                'destroySession': 'false'
            },
            success: function (data) {
                if (data == null)
                    return;

                $('#myModal').modal('hide');
                updateCanvas(editor, data)
            }
        });
    });

    mxVertexHandler.prototype.rotationEnabled = false;
    mxGraphHandler.prototype.guidesEnabled = true;
    mxEditor.prototype.dblClickAction = 'expandCell';
    mxEditor.prototype.strgClickAction = 'goToResource';
    mxGuide.prototype.isEnabledForEvent = function (evt) {
        return !mxEvent.isAltDown(evt);
    };


    mxConnectionHandler.prototype.connectImage = new mxImage('images/DMP/right-arrow.png', 16, 16);

    editor.graph.setConnectable(false);
    editor.graph.setConnectableEdges(false);
    editor.graph.setAllowDanglingEdges(false);
    editor.graph.setDisconnectOnMove(false);

    editor.addAction('expandCell', function (editor, cell) {
        $.ajax({
            type: 'POST',
            url: "./importPROV",
            data: {
                'entityName': cell.id,
                'pathLen': '1',
                'endpoint': document.getElementById("endpoint").value,
                'destroySession': 'false'
            },
            success: function (data) {
                if (data == null)
                    return;

                x_old = cell.geometry.x;
                y_old = cell.geometry.y;
                scale_old = editor.graph.getView().getScale();
                translate_old_x = editor.graph.view.translate.x;
                translate_old_y = editor.graph.view.translate.y;
                
                updateCanvas(editor, data)                

                x_new = editor.graph.model.getCell(cell.id).geometry.x
                y_new = editor.graph.model.getCell(cell.id).geometry.y
                x_diff = x_new - x_old;
                y_diff = y_new - y_old;
                
                editor.graph.view.scaleAndTranslate(scale_old, translate_old_x - x_diff, translate_old_y - y_diff)
                editor.graph.view.rendering = true;
            }
        });
    });
    editor.addAction('goToResource', function (editor, cell) {
        window.open(cell.id)
    })

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
};
