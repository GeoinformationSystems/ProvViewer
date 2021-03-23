import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;

import org.apache.jena.rdfconnection.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;
import java.io.FileOutputStream;

@WebServlet(name = "Prov2Graph")

public class Prov2Graph extends HttpServlet {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static Document ttl2xml (String serviceURI) throws IOException {

		String dataQuery = "SELECT ?s WHERE {?s a <http://www.w3.org/ns/prov#Entity>}";
		String processQuery = "SELECT ?s WHERE {?s a <http://www.w3.org/ns/prov#Activity>}";
		String generatedQuery = "SELECT ?s ?o WHERE {?s <http://www.w3.org/ns/prov#wasGeneratedBy> ?o}";
		String usedQuery = "SELECT ?s ?o WHERE {?s <http://www.w3.org/ns/prov#used> ?o}";
		String agentQuery = "SELECT ?s WHERE {?s a <http://www.w3.org/ns/prov#Agent>}";
		String attributedQuery = "SELECT ?s ?o WHERE {?s <http://www.w3.org/ns/prov#wasAttributedTo> ?o}";
		String associatedQuery = "SELECT ?s ?o WHERE {?s <http://www.w3.org/ns/prov#wasAssociatedWith> ?o}";


		Document document = new Document();
        Element mxGraphModel = new Element("mxGraphModel");

        Element root = new Element("root");

        Element diagram = new Element("Diagram")
            .setAttribute("label", "My Diagram")
            .setAttribute("href", "http://www.jgraph.com/")
            .setAttribute("id", "0");
        diagram.addContent(new Element("mxCell"));

        Element layer = new Element("Layer")
            .setAttribute("label", "Default Layer")
            .setAttribute("id", "1");
        layer.addContent(
            new Element("mxCell")
                .setAttribute("parent", "0"));

        root.addContent(diagram)
            .addContent(layer);


		try (QueryExecution dataQ = QueryExecutionFactory.sparqlService(serviceURI, dataQuery)) {
			ResultSet dataResults = dataQ.execSelect();

			while (dataResults.hasNext()) {
				QuerySolution soln = dataResults.nextSolution();
				RDFNode x = soln.get("s");
			
				String o = x.toString();
				int oLength = o.length() * 6 + 10;
				Element dataDMP = new Element("DataDMP")
					.setAttribute("label", o)
					.setAttribute("id", o)
					.addContent(new Element("mxCell")
						.setAttribute("style", "dataDMP")
						.setAttribute("parent", "1")
						.setAttribute("vertex", "1")                
					.addContent(new Element("mxGeometry")
						.setAttribute("x", "0")
						.setAttribute("y", "0")
						.setAttribute("width", String.valueOf(oLength))
						.setAttribute("height", "40")
						.setAttribute("as", "geometry")));
				root.addContent(dataDMP);				
			}
		}
			
		try (QueryExecution processQ = QueryExecutionFactory.sparqlService(serviceURI, processQuery)) {
			ResultSet processResults = processQ.execSelect();
			while (processResults.hasNext()) {
				QuerySolution soln = processResults.nextSolution();
				RDFNode x = soln.get("s");
	
				String s = x.toString();
				int sLength = s.length()*5 + 10;
				Element processDMP = new Element ("ProcessDMP")
				.setAttribute("label", s)
				.setAttribute("id", s)
				.addContent(new Element ("mxCell")
					.setAttribute("style", "processDMP")
					.setAttribute("vertex", "1")
					.setAttribute("parent", "1")
				.addContent(new Element("mxGeometry")
					.setAttribute("x", "100")
					.setAttribute("y", "0")
					.setAttribute("width", String.valueOf(sLength))
					.setAttribute("height", "40")
					.setAttribute("as", "geometry")));
				root.addContent(processDMP);				
			}
		}

		try (QueryExecution agentQ = QueryExecutionFactory.sparqlService(serviceURI, agentQuery)) {
			ResultSet agentResults = agentQ.execSelect();
			while (agentResults.hasNext()) {
				QuerySolution soln = agentResults.nextSolution();
				RDFNode x = soln.get("s");
	
				String s = x.toString();
				int sLength = s.length()*5+5;
				Element agentDMP = new Element ("AgentDMP")
				.setAttribute("label", s)
				.setAttribute("id", s)
				.addContent(new Element ("mxCell")
					.setAttribute("style", "agentDMP")
					.setAttribute("vertex", "1")
					.setAttribute("parent", "1")
				.addContent(new Element("mxGeometry")
					.setAttribute("x", "100")
					.setAttribute("y", "0")
					.setAttribute("width", String.valueOf(sLength))
					.setAttribute("height", "40")
					.setAttribute("as", "geometry")));
				root.addContent(agentDMP);				
			}
		}

		try (QueryExecution generatedQ = QueryExecutionFactory.sparqlService(serviceURI, generatedQuery)) {
			ResultSet generatedResults = generatedQ.execSelect();
			int id = 2;
			while (generatedResults.hasNext()) {
				QuerySolution soln = generatedResults.nextSolution();
				
				RDFNode s = soln.get("s");
				RDFNode o = soln.get("o");

				String subj = s.toString();
				String obj = o.toString();

				Element connector = new Element ("Connector")
				// .setAttribute("label", "wasGeneratedBy")
				.setAttribute("id", String.valueOf(id))
				.addContent(new Element ("mxCell")
					.setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
					.setAttribute("edge", "1")
					.setAttribute("parent", "1")
					.setAttribute("source", obj)
					.setAttribute("target", subj)
					.addContent(new Element ("mxGeometry")
						.setAttribute("relative", "1")
						.setAttribute("as", "geometry")
						.addContent(new Element ("Array")
							.setAttribute("as", "geometry")
							.addContent(new Element ("mxPoint")
								.setAttribute("x", "0")
								.setAttribute("y", "0"))
							.addContent(new Element ("mxPoint")
								.setAttribute("x", "0")
								.setAttribute("y", "0")))));
				root.addContent(connector);
				id++;
			}
		}

		try (QueryExecution usedQ = QueryExecutionFactory.sparqlService(serviceURI, usedQuery)) {
			ResultSet usedResults = usedQ.execSelect();
			int id = 2;
			while (usedResults.hasNext()) {
				QuerySolution soln = usedResults.nextSolution();
				
				RDFNode s = soln.get("s");
				RDFNode o = soln.get("o");

				String subj = s.toString();
				String obj = o.toString();

				Element connector = new Element ("Connector")
				// .setAttribute("label", "used")
				.setAttribute("id", String.valueOf(id) + "b" )
				.addContent(new Element ("mxCell")
					.setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
					.setAttribute("edge", "1")
					.setAttribute("parent", "1")
					.setAttribute("source", obj)
					.setAttribute("target", subj)
					.addContent(new Element ("mxGeometry")
						.setAttribute("relative", "1")
						.setAttribute("as", "geometry")
						.addContent(new Element ("Array")
							.setAttribute("as", "geometry")
							.addContent(new Element ("mxPoint")
								.setAttribute("x", "0")
								.setAttribute("y", "0"))
							.addContent(new Element ("mxPoint")
								.setAttribute("x", "0")
								.setAttribute("y", "0")))));
				root.addContent(connector);
				id++;
			}
		}	

		try (QueryExecution associatedQ = QueryExecutionFactory.sparqlService(serviceURI, associatedQuery)) {
			ResultSet associatedResults = associatedQ.execSelect();
			int id = 2;
			while (associatedResults.hasNext()) {
				QuerySolution soln = associatedResults.nextSolution();
				
				RDFNode s = soln.get("s");
				RDFNode o = soln.get("o");

				String subj = s.toString();
				String obj = o.toString();

				Element connector = new Element ("Connector")
				// .setAttribute("label", "wasAssociatedWith")
				.setAttribute("id", String.valueOf(id) + "c" )
				.addContent(new Element ("mxCell")
					.setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
					.setAttribute("edge", "1")
					.setAttribute("parent", "1")
					.setAttribute("source", subj)
					.setAttribute("target", obj)
					.addContent(new Element ("mxGeometry")
						.setAttribute("relative", "1")
						.setAttribute("as", "geometry")
						.addContent(new Element ("Array")
							.setAttribute("as", "geometry")
							.addContent(new Element ("mxPoint")
								.setAttribute("x", "0")
								.setAttribute("y", "0"))
							.addContent(new Element ("mxPoint")
								.setAttribute("x", "0")
								.setAttribute("y", "0")))));
				root.addContent(connector);
				id++;
			}
		}

		try (QueryExecution attributedQ = QueryExecutionFactory.sparqlService(serviceURI, attributedQuery)) {
			ResultSet attributedResults = attributedQ.execSelect();
			int id = 2;
			while (attributedResults.hasNext()) {
				QuerySolution soln = attributedResults.nextSolution();
				
				RDFNode s = soln.get("s");
				RDFNode o = soln.get("o");

				String subj = s.toString();
				String obj = o.toString();

				Element connector = new Element ("Connector")
				// .setAttribute("label", "wasAttributedTo")
				.setAttribute("id", String.valueOf(id) + "d" )
				.addContent(new Element ("mxCell")
					.setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
					.setAttribute("edge", "1")
					.setAttribute("parent", "1")
					.setAttribute("source", subj)
					.setAttribute("target", obj)
					.addContent(new Element ("mxGeometry")
						.setAttribute("relative", "1")
						.setAttribute("as", "geometry")
						.addContent(new Element ("Array")
							.setAttribute("as", "geometry")
							.addContent(new Element ("mxPoint")
								.setAttribute("x", "0")
								.setAttribute("y", "0"))
							.addContent(new Element ("mxPoint")
								.setAttribute("x", "0")
								.setAttribute("y", "0")))));
				root.addContent(connector);
				id++;
			}
		}
		
		mxGraphModel.addContent(root);
        document.setContent(mxGraphModel);
        return document;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
                try {
                    Document doc = ttl2xml("http://172.26.62.72:8080/fuseki/provenance_graph_example");

                    response.setContentType("text/plain");
                    response.setHeader("Content-Disposition",
                            "attachment");
                    response.setStatus(HttpServletResponse.SC_OK);
                    OutputStream out = response.getOutputStream();
                    new XMLOutputter(Format.getPrettyFormat()).output(doc, out);
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
