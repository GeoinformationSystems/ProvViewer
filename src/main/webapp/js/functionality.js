var mouseIsOverCanvas = false;

$(document).ready(function () {

    getDatasets();
    // setTimeout(() => processURLparams(), 1000);
    $("#getDatasets").bind("click", getDatasets);

    $("#editorarea").bind({
        mouseenter: function () {
            mouseIsOverCanvas = true;
        },
        mouseleave: function () {
            mouseIsOverCanvas = false;
        }
    });

});

function processURLparams() {
    var parameters = (function (queryString = document.location.search) {
        var dictionary = {};
        // remove the '?' from the beginning of the
        // if it exists
        if (queryString.indexOf('?') === 0) {
            queryString = queryString.substr(1);
        }

        // Step 1: separate out each key/value pair
        var parts = queryString.split('&');

        for (var i = 0; i < parts.length; i++) {
            var p = parts[i];
            // Step 2: Split Key/Value pair
            var keyValuePair = p.split('=');

            // Step 3: Add Key/Value pair to Dictionary object
            var key = keyValuePair[0];
            var value = keyValuePair[1];

            // decode URI encoded string
            value = decodeURIComponent(value);
            value = value.replace(/\+/g, ' ');

            dictionary[key] = value;
        }

        // Step 4: Return Dictionary Object
        return dictionary;
    })();

    if (parameters["endpoint"]) {
        document.getElementById("endpoint").value = parameters["endpoint"];
    }
    if (parameters["dataset"]) {
        datasetId = String(parameters["dataset"]);
        document.getElementById(datasetId).selected = true;
    }
    if (parameters["endpoint"] && parameters["dataset"]) {
        $("#newGraph").click();
    }

}

function getDatasets() {
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
        let select = document.getElementById("selectionBox");
        for (let i = 0; i < results.length; i++) {
            option = document.createElement("option")
            option.value = option.id = (results[i]["entity"]["value"]);
            if (results[i]["entityTitle"] != null) {
                option.text = (results[i]["entityTitle"]["value"]);
            } else {
                option.text = (results[i]["entity"]["value"]);
            }

            selectionBox.add(option);

        }
        processURLparams()
    });
}

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
        // shift graph to mid of editor area 
        var translate = ($('#editorarea').height() / (2 * editor.graph.view.getScale()) - (editor.graph.view.getGraphBounds().y + editor.graph.view.getGraphBounds().height / 2));
        editor.graph.view.setTranslate(0, translate)
        editor.graph.view.rendering = true;
        editor.graph.refresh();
    }
}

function onInit(editor) {
    $("#newGraph").bind("click", function (e) {
        // console.log(document.getElementById("selectionBox").options[document.getElementById("selectionBox").selectedIndex].value)
        $.ajax({
            type: 'POST',
            url: "./importPROV",
            data: {
                'entityId': document.getElementById("selectionBox").options[document.getElementById("selectionBox").selectedIndex].value,
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
                'entityId': document.getElementById("selectionBox").options[document.getElementById("selectionBox").selectedIndex].value,
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
                'entityId': cell.id,
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
            if (mouseIsOverCanvas) {
                if (up) {
                    editor.execute('zoomIn');
                }
                else {
                    editor.execute('zoomOut');
                }
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
        // shift graph to mid of editor area 
        // var translate = ($('#editorarea').height() / (2*editor.graph.view.getScale()) - (editor.graph.view.getGraphBounds().y + editor.graph.view.getGraphBounds().height / 2));
        // editor.graph.view.setTranslate(-editor.graph.view.getGraphBounds().x, translate)
    });
    mxEvent.addListener(document.getElementById("hierarchicalButton"), 'click', function () {
        var layout = new mxHierarchicalLayout(editor.graph, mxConstants.DIRECTION_WEST, true);
        layout.interHierarchySpacing = 30;
        layout.interRankCellSpacing = 30;
        layout.intraCellSpacing = 30;
        var parent = editor.graph.getDefaultParent();
        layout.execute(parent);
        editor.graph.fit();
        editor.graph.refresh();
        // shift graph to mid of editor area 
        var translate = ($('#editorarea').height() / (2 * editor.graph.view.getScale()) - (editor.graph.view.getGraphBounds().y + editor.graph.view.getGraphBounds().height / 2));
        editor.graph.view.setTranslate(0, translate)
        editor.graph.view.rendering = true;

        editor.graph.refresh();
    });
};
