import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.*;
import java.net.URLDecoder;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.jena.rdfconnection.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.riot.Lang;
import java.io.FileOutputStream;

@WebServlet(name = "Prov2Graph")

public class Prov2Graph extends HttpServlet {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static Model constructSubGraph(Model model, String serviceURI, Set<String> entityNames, int length)
			throws IOException {

		String PROV = "http://www.w3.org/ns/prov#";
		model.setNsPrefix("prov", PROV);
		while (length > 0) {
			for (String entityName : entityNames) {
				Resource entity = model.createResource(entityName);
				entity.addProperty(RDF.type, model.createResource(PROV + "Entity"));
				String query;

				// String attributedQuery = "SELECT ?s ?o WHERE {?s
				// <http://www.w3.org/ns/prov#wasAttributedTo> ?o}";
				// String associatedQuery = "SELECT ?s ?o WHERE {?s
				// <http://www.w3.org/ns/prov#wasAssociatedWith> ?o}";

				query = String.format(
						"SELECT ?activity WHERE {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity}",
						entityName);
				try (QueryExecution wasGeneratedByQ = QueryExecutionFactory.sparqlService(serviceURI, query)) {
					ResultSet wasGeneratedByResults = wasGeneratedByQ.execSelect();
					while (wasGeneratedByResults.hasNext()) {
						QuerySolution soln = wasGeneratedByResults.nextSolution();
						Resource s = model.createResource(soln.get("activity").toString());
						entity.addProperty(model.createProperty(PROV + "wasGeneratedBy"), soln.get("activity"));
						s.addProperty(RDF.type, model.createResource(PROV + "Activity"));
					}
				}

				query = String.format("SELECT ?activity WHERE {?activity <http://www.w3.org/ns/prov#used> <%s>. }",
						entityName);
				try (QueryExecution usedQ = QueryExecutionFactory.sparqlService(serviceURI, query)) {
					ResultSet usedResults = usedQ.execSelect();
					while (usedResults.hasNext()) {
						QuerySolution soln = usedResults.nextSolution();
						Resource s = model.createResource(soln.get("activity").toString());
						s.addProperty(model.createProperty(PROV + "used"), entity);
						s.addProperty(RDF.type, model.createResource(PROV + "Activity"));
					}
				}

				query = String.format(
						"SELECT ?entity ?activity WHERE {?entity <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> <%s>. }",
						entityName);
				try (QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI, query)) {
					ResultSet results = q.execSelect();
					while (results.hasNext()) {
						QuerySolution soln = results.nextSolution();
						Resource s = model.createResource(soln.get("entity").toString());
						s.addProperty(model.createProperty(PROV + "wasGeneratedBy"), soln.getResource("activity"));
						s.addProperty(RDF.type, model.createResource(PROV + "Entity"));
						entityNames.add(soln.get("entity").toString());
					}
				}

				query = String.format(
						"SELECT ?entity ?activity WHERE {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> ?entity.  }",
						entityName);
				try (QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI, query)) {
					ResultSet results = q.execSelect();
					while (results.hasNext()) {
						QuerySolution soln = results.nextSolution();
						// System.out.println(soln.getResource("activity"));
						Resource s = model.createResource(soln.get("activity").toString());
						Resource t = model.createResource(soln.get("entity").toString());
						s.addProperty(model.createProperty(PROV + "used"), t);
						t.addProperty(RDF.type, model.createResource(PROV + "Entity"));
						entityNames.add(soln.get("entity").toString());
					}
				}
			}
			length -= 1;
		}
		// for (String name : entityNames) {
		// System.out.println(name);
		// }
		return model;
	}

	public static Document prov2mxGraph(Model model) throws IOException {

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

		Element diagram = new Element("Diagram").setAttribute("label", "My Diagram")
				.setAttribute("href", "http://www.jgraph.com/").setAttribute("id", "0");
		diagram.addContent(new Element("mxCell"));

		Element layer = new Element("Layer").setAttribute("label", "Default Layer").setAttribute("id", "1");
		layer.addContent(new Element("mxCell").setAttribute("parent", "0"));

		root.addContent(diagram).addContent(layer);

		try (QueryExecution dataQ = QueryExecutionFactory.create(dataQuery, model)) {
			ResultSet dataResults = dataQ.execSelect();

			while (dataResults.hasNext()) {
				QuerySolution soln = dataResults.nextSolution();
				RDFNode x = soln.get("s");

				String o = x.toString();
				int oLength = o.length() * 6 + 10;
				Element dataDMP = new Element("DataDMP").setAttribute("label", o).setAttribute("id", o)
						.addContent(new Element("mxCell").setAttribute("style", "dataDMP").setAttribute("parent", "1")
								.setAttribute("vertex", "1")
								.addContent(new Element("mxGeometry").setAttribute("x", "0").setAttribute("y", "0")
										.setAttribute("width", String.valueOf(oLength)).setAttribute("height", "40")
										.setAttribute("as", "geometry")));
				root.addContent(dataDMP);
			}
		}

		try (QueryExecution processQ = QueryExecutionFactory.create(processQuery, model)) {
			ResultSet processResults = processQ.execSelect();
			while (processResults.hasNext()) {
				QuerySolution soln = processResults.nextSolution();
				RDFNode x = soln.get("s");

				String s = x.toString();
				int sLength = s.length() * 5 + 10;
				Element processDMP = new Element("ProcessDMP").setAttribute("label", s).setAttribute("id", s)
						.addContent(new Element("mxCell").setAttribute("style", "processDMP")
								.setAttribute("vertex", "1").setAttribute("parent", "1")
								.addContent(new Element("mxGeometry").setAttribute("x", "100").setAttribute("y", "0")
										.setAttribute("width", String.valueOf(sLength)).setAttribute("height", "40")
										.setAttribute("as", "geometry")));
				root.addContent(processDMP);
			}
		}

		try (QueryExecution agentQ = QueryExecutionFactory.create(agentQuery, model)) {
			ResultSet agentResults = agentQ.execSelect();
			while (agentResults.hasNext()) {
				QuerySolution soln = agentResults.nextSolution();
				RDFNode x = soln.get("s");

				String s = x.toString();
				int sLength = s.length() * 5 + 5;
				Element agentDMP = new Element("AgentDMP").setAttribute("label", s).setAttribute("id", s)
						.addContent(new Element("mxCell").setAttribute("style", "agentDMP").setAttribute("vertex", "1")
								.setAttribute("parent", "1")
								.addContent(new Element("mxGeometry").setAttribute("x", "100").setAttribute("y", "0")
										.setAttribute("width", String.valueOf(sLength)).setAttribute("height", "40")
										.setAttribute("as", "geometry")));
				root.addContent(agentDMP);
			}
		}

		try (QueryExecution generatedQ = QueryExecutionFactory.create(generatedQuery, model)) {
			ResultSet generatedResults = generatedQ.execSelect();
			int id = 2;
			while (generatedResults.hasNext()) {
				QuerySolution soln = generatedResults.nextSolution();

				RDFNode s = soln.get("s");
				RDFNode o = soln.get("o");

				String subj = s.toString();
				String obj = o.toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "wasGeneratedBy")
						.setAttribute("id", String.valueOf(id))
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source", obj)
								.setAttribute("target", subj)
								.addContent(new Element("mxGeometry").setAttribute("relative", "1")
										.setAttribute("as", "geometry")
										.addContent(new Element("Array").setAttribute("as", "geometry")
												.addContent(new Element("mxPoint").setAttribute("x", "0")
														.setAttribute("y", "0"))
												.addContent(new Element("mxPoint").setAttribute("x", "0")
														.setAttribute("y", "0")))));
				root.addContent(connector);
				id++;
			}
		}

		try (QueryExecution usedQ = QueryExecutionFactory.create(usedQuery, model)) {
			ResultSet usedResults = usedQ.execSelect();
			int id = 2;
			while (usedResults.hasNext()) {
				QuerySolution soln = usedResults.nextSolution();

				RDFNode s = soln.get("s");
				RDFNode o = soln.get("o");

				String subj = s.toString();
				String obj = o.toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "used")
						.setAttribute("id", String.valueOf(id) + "b")
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source", obj)
								.setAttribute("target", subj)
								.addContent(new Element("mxGeometry").setAttribute("relative", "1")
										.setAttribute("as", "geometry")
										.addContent(new Element("Array").setAttribute("as", "geometry")
												.addContent(new Element("mxPoint").setAttribute("x", "0")
														.setAttribute("y", "0"))
												.addContent(new Element("mxPoint").setAttribute("x", "0")
														.setAttribute("y", "0")))));
				root.addContent(connector);
				id++;
			}
		}

		try (QueryExecution associatedQ = QueryExecutionFactory.create(associatedQuery, model)) {
			ResultSet associatedResults = associatedQ.execSelect();
			int id = 2;
			while (associatedResults.hasNext()) {
				QuerySolution soln = associatedResults.nextSolution();

				RDFNode s = soln.get("s");
				RDFNode o = soln.get("o");

				String subj = s.toString();
				String obj = o.toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "wasAssociatedWith")
						.setAttribute("id", String.valueOf(id) + "c")
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source", subj)
								.setAttribute("target", obj)
								.addContent(new Element("mxGeometry").setAttribute("relative", "1")
										.setAttribute("as", "geometry")
										.addContent(new Element("Array").setAttribute("as", "geometry")
												.addContent(new Element("mxPoint").setAttribute("x", "0")
														.setAttribute("y", "0"))
												.addContent(new Element("mxPoint").setAttribute("x", "0")
														.setAttribute("y", "0")))));
				root.addContent(connector);
				id++;
			}
		}

		try (QueryExecution attributedQ = QueryExecutionFactory.create(attributedQuery, model)) {
			ResultSet attributedResults = attributedQ.execSelect();
			int id = 2;
			while (attributedResults.hasNext()) {
				QuerySolution soln = attributedResults.nextSolution();

				RDFNode s = soln.get("s");
				RDFNode o = soln.get("o");

				String subj = s.toString();
				String obj = o.toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "wasAttributedTo")
						.setAttribute("id", String.valueOf(id) + "d")
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source", subj)
								.setAttribute("target", obj)
								.addContent(new Element("mxGeometry").setAttribute("relative", "1")
										.setAttribute("as", "geometry")
										.addContent(new Element("Array").setAttribute("as", "geometry")
												.addContent(new Element("mxPoint").setAttribute("x", "0")
														.setAttribute("y", "0"))
												.addContent(new Element("mxPoint").setAttribute("x", "0")
														.setAttribute("y", "0")))));
				root.addContent(connector);
				id++;
			}
		}

		mxGraphModel.addContent(root);
		document.setContent(mxGraphModel);
		return document;
	}

	// public static Document prov2mxGraph(String serviceURI) throws IOException {

	// String dataQuery = "SELECT ?s WHERE {?s a
	// <http://www.w3.org/ns/prov#Entity>}";
	// String processQuery = "SELECT ?s WHERE {?s a
	// <http://www.w3.org/ns/prov#Activity>}";
	// String generatedQuery = "SELECT ?s ?o WHERE {?s
	// <http://www.w3.org/ns/prov#wasGeneratedBy> ?o}";
	// String usedQuery = "SELECT ?s ?o WHERE {?s <http://www.w3.org/ns/prov#used>
	// ?o}";
	// String agentQuery = "SELECT ?s WHERE {?s a
	// <http://www.w3.org/ns/prov#Agent>}";
	// String attributedQuery = "SELECT ?s ?o WHERE {?s
	// <http://www.w3.org/ns/prov#wasAttributedTo> ?o}";
	// String associatedQuery = "SELECT ?s ?o WHERE {?s
	// <http://www.w3.org/ns/prov#wasAssociatedWith> ?o}";

	// Document document = new Document();
	// Element mxGraphModel = new Element("mxGraphModel");

	// Element root = new Element("root");

	// Element diagram = new Element("Diagram").setAttribute("label", "My Diagram")
	// .setAttribute("href", "http://www.jgraph.com/").setAttribute("id", "0");
	// diagram.addContent(new Element("mxCell"));

	// Element layer = new Element("Layer").setAttribute("label", "Default
	// Layer").setAttribute("id", "1");
	// layer.addContent(new Element("mxCell").setAttribute("parent", "0"));

	// root.addContent(diagram).addContent(layer);

	// try (QueryExecution dataQ = QueryExecutionFactory.sparqlService(serviceURI,
	// dataQuery)) {
	// ResultSet dataResults = dataQ.execSelect();

	// while (dataResults.hasNext()) {
	// QuerySolution soln = dataResults.nextSolution();
	// RDFNode x = soln.get("s");

	// String o = x.toString();
	// int oLength = o.length() * 6 + 10;
	// Element dataDMP = new Element("DataDMP").setAttribute("label",
	// o).setAttribute("id", o)
	// .addContent(new Element("mxCell").setAttribute("style",
	// "dataDMP").setAttribute("parent", "1")
	// .setAttribute("vertex", "1")
	// .addContent(new Element("mxGeometry").setAttribute("x",
	// "0").setAttribute("y", "0")
	// .setAttribute("width", String.valueOf(oLength)).setAttribute("height", "40")
	// .setAttribute("as", "geometry")));
	// root.addContent(dataDMP);
	// }
	// }

	// try (QueryExecution processQ =
	// QueryExecutionFactory.sparqlService(serviceURI, processQuery)) {
	// ResultSet processResults = processQ.execSelect();
	// while (processResults.hasNext()) {
	// QuerySolution soln = processResults.nextSolution();
	// RDFNode x = soln.get("s");

	// String s = x.toString();
	// int sLength = s.length() * 5 + 10;
	// Element processDMP = new Element("ProcessDMP").setAttribute("label",
	// s).setAttribute("id", s)
	// .addContent(new Element("mxCell").setAttribute("style", "processDMP")
	// .setAttribute("vertex", "1").setAttribute("parent", "1")
	// .addContent(new Element("mxGeometry").setAttribute("x",
	// "100").setAttribute("y", "0")
	// .setAttribute("width", String.valueOf(sLength)).setAttribute("height", "40")
	// .setAttribute("as", "geometry")));
	// root.addContent(processDMP);
	// }
	// }

	// try (QueryExecution agentQ = QueryExecutionFactory.sparqlService(serviceURI,
	// agentQuery)) {
	// ResultSet agentResults = agentQ.execSelect();
	// while (agentResults.hasNext()) {
	// QuerySolution soln = agentResults.nextSolution();
	// RDFNode x = soln.get("s");

	// String s = x.toString();
	// int sLength = s.length() * 5 + 5;
	// Element agentDMP = new Element("AgentDMP").setAttribute("label",
	// s).setAttribute("id", s)
	// .addContent(new Element("mxCell").setAttribute("style",
	// "agentDMP").setAttribute("vertex", "1")
	// .setAttribute("parent", "1")
	// .addContent(new Element("mxGeometry").setAttribute("x",
	// "100").setAttribute("y", "0")
	// .setAttribute("width", String.valueOf(sLength)).setAttribute("height", "40")
	// .setAttribute("as", "geometry")));
	// root.addContent(agentDMP);
	// }
	// }

	// try (QueryExecution generatedQ =
	// QueryExecutionFactory.sparqlService(serviceURI, generatedQuery)) {
	// ResultSet generatedResults = generatedQ.execSelect();
	// int id = 2;
	// while (generatedResults.hasNext()) {
	// QuerySolution soln = generatedResults.nextSolution();

	// RDFNode s = soln.get("s");
	// RDFNode o = soln.get("o");

	// String subj = s.toString();
	// String obj = o.toString();

	// Element connector = new Element("Connector")
	// // .setAttribute("label", "wasGeneratedBy")
	// .setAttribute("id", String.valueOf(id))
	// .addContent(new Element("mxCell").setAttribute("style",
	// "noEdgeStyle=1;orthogonal=1;")
	// .setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source",
	// obj)
	// .setAttribute("target", subj)
	// .addContent(new Element("mxGeometry").setAttribute("relative", "1")
	// .setAttribute("as", "geometry")
	// .addContent(new Element("Array").setAttribute("as", "geometry")
	// .addContent(new Element("mxPoint").setAttribute("x", "0")
	// .setAttribute("y", "0"))
	// .addContent(new Element("mxPoint").setAttribute("x", "0")
	// .setAttribute("y", "0")))));
	// root.addContent(connector);
	// id++;
	// }
	// }

	// try (QueryExecution usedQ = QueryExecutionFactory.sparqlService(serviceURI,
	// usedQuery)) {
	// ResultSet usedResults = usedQ.execSelect();
	// int id = 2;
	// while (usedResults.hasNext()) {
	// QuerySolution soln = usedResults.nextSolution();

	// RDFNode s = soln.get("s");
	// RDFNode o = soln.get("o");

	// String subj = s.toString();
	// String obj = o.toString();

	// Element connector = new Element("Connector")
	// // .setAttribute("label", "used")
	// .setAttribute("id", String.valueOf(id) + "b")
	// .addContent(new Element("mxCell").setAttribute("style",
	// "noEdgeStyle=1;orthogonal=1;")
	// .setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source",
	// obj)
	// .setAttribute("target", subj)
	// .addContent(new Element("mxGeometry").setAttribute("relative", "1")
	// .setAttribute("as", "geometry")
	// .addContent(new Element("Array").setAttribute("as", "geometry")
	// .addContent(new Element("mxPoint").setAttribute("x", "0")
	// .setAttribute("y", "0"))
	// .addContent(new Element("mxPoint").setAttribute("x", "0")
	// .setAttribute("y", "0")))));
	// root.addContent(connector);
	// id++;
	// }
	// }

	// try (QueryExecution associatedQ =
	// QueryExecutionFactory.sparqlService(serviceURI, associatedQuery)) {
	// ResultSet associatedResults = associatedQ.execSelect();
	// int id = 2;
	// while (associatedResults.hasNext()) {
	// QuerySolution soln = associatedResults.nextSolution();

	// RDFNode s = soln.get("s");
	// RDFNode o = soln.get("o");

	// String subj = s.toString();
	// String obj = o.toString();

	// Element connector = new Element("Connector")
	// // .setAttribute("label", "wasAssociatedWith")
	// .setAttribute("id", String.valueOf(id) + "c")
	// .addContent(new Element("mxCell").setAttribute("style",
	// "noEdgeStyle=1;orthogonal=1;")
	// .setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source",
	// subj)
	// .setAttribute("target", obj)
	// .addContent(new Element("mxGeometry").setAttribute("relative", "1")
	// .setAttribute("as", "geometry")
	// .addContent(new Element("Array").setAttribute("as", "geometry")
	// .addContent(new Element("mxPoint").setAttribute("x", "0")
	// .setAttribute("y", "0"))
	// .addContent(new Element("mxPoint").setAttribute("x", "0")
	// .setAttribute("y", "0")))));
	// root.addContent(connector);
	// id++;
	// }
	// }

	// try (QueryExecution attributedQ =
	// QueryExecutionFactory.sparqlService(serviceURI, attributedQuery)) {
	// ResultSet attributedResults = attributedQ.execSelect();
	// int id = 2;
	// while (attributedResults.hasNext()) {
	// QuerySolution soln = attributedResults.nextSolution();

	// RDFNode s = soln.get("s");
	// RDFNode o = soln.get("o");

	// String subj = s.toString();
	// String obj = o.toString();

	// Element connector = new Element("Connector")
	// // .setAttribute("label", "wasAttributedTo")
	// .setAttribute("id", String.valueOf(id) + "d")
	// .addContent(new Element("mxCell").setAttribute("style",
	// "noEdgeStyle=1;orthogonal=1;")
	// .setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source",
	// subj)
	// .setAttribute("target", obj)
	// .addContent(new Element("mxGeometry").setAttribute("relative", "1")
	// .setAttribute("as", "geometry")
	// .addContent(new Element("Array").setAttribute("as", "geometry")
	// .addContent(new Element("mxPoint").setAttribute("x", "0")
	// .setAttribute("y", "0"))
	// .addContent(new Element("mxPoint").setAttribute("x", "0")
	// .setAttribute("y", "0")))));
	// root.addContent(connector);
	// id++;
	// }
	// }

	// mxGraphModel.addContent(root);
	// document.setContent(mxGraphModel);
	// return document;
	// }

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Model model;
		HttpSession session = request.getSession();
		if (session.getAttribute("model") != null) {
			model = (Model) session.getAttribute("model");
		} else {
			model = ModelFactory.createDefaultModel();
		}
		try {

			String entityName = request.getParameter("entityName");
			String endpoint = request.getParameter("endpoint");
			System.out.println("endpoint");
			System.out.println(endpoint);
			int pathLen = Integer.parseInt(request.getParameter("pathLen"));
			Set<String> entities = new LinkedHashSet();
			entities.add(entityName);
			model = constructSubGraph(model, endpoint, entities,
					pathLen);
			Document doc = prov2mxGraph(model);
			response.setContentType("text/plain");
			response.setHeader("Content-Disposition", "attachment");
			response.setStatus(HttpServletResponse.SC_OK);
			OutputStream out = response.getOutputStream();
			new XMLOutputter(Format.getPrettyFormat()).output(doc, out);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("dsff");
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}
}
