/**
 * Constructs a new metadata dialog.
 */
var EditDataDialog = function (editor, cell) {
    var div = document.createElement('div');
    var graph = editor.graph;

    div.style.height = '310px';
    div.style.overflow = 'auto';

    var value = graph.getModel().getValue(cell);

    // Converts the value to an XML node
    if (!mxUtils.isNode(value)) {
        var doc = mxUtils.createXmlDocument();
        var obj = doc.createElement('object');
        obj.setAttribute('label', value || '');
        value = obj;
    }

    // Creates the dialog contents
    var form = new mxForm('properties');
    form.table.style.width = '95%';
    form.table.style.paddingRight = '20px';

    var attrs = value.attributes;
    var names = [];
    var texts = [];
    var count = 0;

    var addRemoveButton = function (text, name) {
        text.parentNode.style.marginRight = '12px';

        var removeAttr = document.createElement('a');
        var img = mxUtils.createImage(EditDataDialog.prototype.closeImage);
        img.style.height = '14px';
        img.style.fontSize = '14px';
        img.style.marginBottom = (mxClient.IS_IE11) ? '-1px' : '14px';

        removeAttr.className = 'geButton';
        removeAttr.setAttribute('title', mxResources.get('delete'));
        removeAttr.style.margin = '0px';
        removeAttr.style.width = '24px';
        removeAttr.style.height = '24px';
        removeAttr.style.fontSize = '24px';
        removeAttr.style.cursor = 'pointer';
        removeAttr.style.marginLeft = '6px';
        removeAttr.appendChild(img);

        var removeAttrFn = (function (name) {
            return function () {
                var count = 0;

                for (var j = 0; j < names.length; j++) {
                    if (names[j] == name) {
                        texts[j] = null;
                        form.table.deleteRow(count);

                        break;
                    }

                    if (texts[j] != null) {
                        count++;
                    }
                }
            };
        })(name);

        mxEvent.addListener(removeAttr, 'click', removeAttrFn);

        text.parentNode.style.whiteSpace = 'nowrap';
        text.parentNode.appendChild(removeAttr);
    };

    var addTextArea = function (index, name, value) {
        names[index] = name;
        texts[index] = form.addTextarea(names[count] + ':', value, 2);
        texts[index].style.width = '100%';

        addRemoveButton(texts[index], name);
    };

    var temp = [];

    for (var i = 0; i < attrs.length; i++) {
        if (attrs[i].nodeName != 'label' && attrs[i].nodeName != 'placeholders' && attrs[i].nodeName != 'href') {
            temp.push({name: attrs[i].nodeName, value: attrs[i].nodeValue});
        }
    }

    // Sorts by name
    temp.sort(function (a, b) {
        if (a.name < b.name) {
            return -1;
        }
        else if (a.name > b.name) {
            return 1;
        }
        else {
            return 0;
        }
    });

    for (var i = 0; i < temp.length; i++) {
        addTextArea(count, temp[i].name, temp[i].value);
        count++;
    }

    div.appendChild(form.table);
    div.appendChild(document.createElement('hr'));

    var newProp = document.createElement('div');
    newProp.style.whiteSpace = 'nowrap';
    newProp.style.marginTop = '6px';

    var nameInput = document.createElement('input');
    nameInput.setAttribute('placeholder', mxResources.get('enterMetadataName'));
    nameInput.setAttribute('type', 'text');
    nameInput.setAttribute('size', (mxClient.IS_IE || mxClient.IS_IE11) ? '18' : '22');
    nameInput.style.marginLeft = '2px';
    nameInput.style.marginRight = '10px';

    newProp.appendChild(nameInput);
    div.appendChild(newProp);

    var addBtn = mxUtils.button(mxResources.get('addProperty'), function () {
        var name = nameInput.value;

        // Avoid ':' in attribute names which seems to be valid in Chrome
        if (name.length > 0 && name != 'label' && name != 'placeholders' && name.indexOf(':') < 0) {
            try {
                var idx = mxUtils.indexOf(names, name);

                if (idx >= 0 && texts[idx] != null) {
                    texts[idx].focus();
                }
                else {
                    // Checks if the name is valid
                    var clone = value.cloneNode(false);
                    clone.setAttribute(name, '');

                    if (idx >= 0) {
                        names.splice(idx, 1);
                        texts.splice(idx, 1);
                    }

                    names.push(name);
                    var text = form.addTextarea(name + ':', '', 2);
                    text.style.width = '100%';
                    texts.push(text);
                    addRemoveButton(text, name);

                    text.focus();
                }

                nameInput.value = '';
                updateAddBtn();
            }
            catch (e) {
                mxUtils.alert(e);
            }
        }
        else {
            mxUtils.alert(mxResources.get('invalidName'));
        }
    });

    this.init = function () {
        if (texts.length > 0) {
            texts[0].focus();
        }
        else {
            nameInput.focus();
        }
    };

    addBtn.setAttribute('disabled', 'disabled');
    addBtn.className = 'btn btn-primary';
    addBtn.setAttribute('type', 'button');
    newProp.appendChild(addBtn);

    var cancelBtn = mxUtils.button(mxResources.get('cancel'), function () {
        $('#editMetadataModal').hide();

    });
    cancelBtn.className = 'btn btn-secondary';
    cancelBtn.setAttribute('type', 'button');
    cancelBtn.setAttribute('data-dismiss', 'modal');

    var applyBtn = mxUtils.button(mxResources.get('apply'), function () {
        try {
            // Clones and updates the value
            value = value.cloneNode(true);
            var removeLabel = false;

            for (var i = 0; i < names.length; i++) {
                if (texts[i] == null) {
                    value.removeAttribute(names[i]);
                }
                else {
                    value.setAttribute(names[i], texts[i].value);
                    removeLabel = removeLabel || (names[i] == 'placeholder' &&
                        value.getAttribute('placeholders') == '1');
                }
            }

            // Removes label if placeholder is assigned
            if (removeLabel) {
                value.removeAttribute('label');
            }

            // Updates the value of the cell (undoable)
            graph.getModel().setValue(cell, value);

            $('#editMetadataModal').hide();
        }
        catch (e) {
            mxUtils.alert(e);
        }
    });
    applyBtn.className = 'btn btn-primary';
    applyBtn.setAttribute('type', 'button');
    applyBtn.setAttribute('data-dismiss', 'modal');

    function updateAddBtn() {
        if (nameInput.value.length > 0) {
            addBtn.removeAttribute('disabled');
        }
        else {
            addBtn.setAttribute('disabled', 'disabled');
        }
    };

    mxEvent.addListener(nameInput, 'keyup', updateAddBtn);

    // Catches all changes that don't fire a keyup (such as paste via mouse)
    mxEvent.addListener(nameInput, 'change', updateAddBtn);

    var modalContent = $('#editMetadataContent');
    var modalFooter = $('#editMetadataFooter');
    modalFooter.empty();
    modalFooter.append(cancelBtn);
    modalFooter.append(applyBtn);
    modalContent.empty();
    modalContent.append(div);
};

EditDataDialog.prototype.closeImage = (!mxClient.IS_SVG) ? 'images/symbols/cancel_end.png' : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAkAAAAJAQMAAADaX5RTAAAABlBMVEV7mr3///+wksspAAAAAnRSTlP/AOW3MEoAAAAdSURBVAgdY9jXwCDDwNDRwHCwgeExmASygSL7GgB12QiqNHZZIwAAAABJRU5ErkJggg==';