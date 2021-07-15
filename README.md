# PROV-Viewer

_Software is still under development. A running instance can be reached at: https://geokur-dmp2.geo.tu-dresden.de/provViewer/._

Proof-of-concept application, that allows a user to visualize provenance graphs that follow the PROV-Ontology. Users can either directly query a SPARQL endpoint that contains provenance information or upload a valid RDF file. Provenance descriptions should always follow the PROV-O starting point classes and properties [https://www.w3.org/TR/prov-o/#description-starting-point-terms, https://www.w3.org/TR/prov-o/#starting-points-figure].

PROV Entities are indicated with red color, Activities with blue. Agents are not implemented yet. Entities that show up in light red, can be expanded with a double click or right click.

The default SPARQL endpoint points to an RDF serialization of the metadata catalog [https://geokur-dmp.geo.tu-dresden.de/] of the GeoKur research project [https://geokur.geo.tu-dresden.de/]. The Metadata Scheme of this catalog supports the tracing of provenance. 

An example upload is provided by `SuitablityCorridorModel-Prov.ttl`. The example shows the provenance of the processing steps of an ArcGIS Tutorial [https://learn.arcgis.com/en/projects/build-a-model-to-connect-mountain-lion-habitat/]. The RDF file is generated from the ArcGIS model report, which was converted with the tool ArcGIS2ProvO [https://github.com/GeoinformationSystems/ArcGIS2ProvO].

## Structure

ProvViewer is written in Java and JavaScript. 

- __src/main__
    - __java__: Contains Java Servlet classes that form the backend of the application. They make use of the Apache Jena RDF library to construct subsets of the available provenance information. These subsets are converted to mxGraph objects and served to the client.
    - __webapp__
        - __js__: Frontend JS. Sends requests for subsets of the available provenance information to the backend and visualizes the response accordingly.
        - __webinf__\
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;web.xml: Servlet mapping.\
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;index.html: Website corpus.
- pom.xml: Maven dependencies.
- provViewer.war: Compiled and deployable (e.g. on Apache Tomcat) build of the application.

## Used Libraries

### Java

- JDOM2: http://www.jdom.org/, https://mvnrepository.com/artifact/org.jdom/jdom2
    - Apache-style open source license
- Javax-Servlet-API 3.1.0: ,https://mvnrepository.com/artifact/javax.servlet/javax.servlet-api/3.1.0
    - GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License")
- Apache Jena: https://jena.apache.org/, https://mvnrepository.com/artifact/org.apache.jena/apache-jena-libs
    - Apache License, Version 2.0

### JavaScript
 - Bootstrap: https://getbootstrap.com/
    - MIT Licence
 - JQuery: https://jquery.com/
    - MIT Licence
 - JGraph/ MxGraph: https://jgraph.github.io/mxgraph/
    - Apache License 2.0


## Contact
Arne Rümmler (arne.ruemmler@tu-dresden.de)

ALWAYS ALT+F5 after deploying a new war file. Browsers might cache information from previous versions!