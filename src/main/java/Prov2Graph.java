import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

@WebServlet(name = "Prov2Graph")

public class Prov2Graph extends HttpServlet {
	/**
	Class that queries a triplestore for provenance information according to PROV-O and generates 
	a visualizable mxGraph object. 
	Method constructSubGraph: 
		Takes a list RDF resources as input and queries for related provenance information of them.
		The queried resources are added to a local RDF subgraph of the original graph, that contains only 
		provenance resources that shall be visualized. If there already exists a subgraph in the current 
		session, the queried provenance resources are added to this subgraph. Otherwise a new subgraph is 
		created. 
		The parameter length is experimental; tested with length = 1. It determines the number of provenance 
		steps that is added to the subgraph for each resource. I.e., if length = 1; for each resource in the 
		provenance graph, only the direct neighbors (identified by 'wasDerivedFrom') of the resource are added 
		to the subgraph.
	Method prov2mxGraph:
		Takes the local RDF subgraph and converts it into an mxGraph object. Furthermore, for each resource 
		in the local RDF subgraph, the original RDF graph is queried for information to enrich the mxGraph
		object with metadata (currently common RDF labels [rdfs:label, skos:prefLabel, dct:title]).
	The methods are called in doPost and the resulting mxGraph object is returned to the client.
	**/
	private static final long serialVersionUID = 1L;

	public static Model constructSubGraph(Model model, String serviceURI, Set<String> entityIds, int length)
			throws IOException {

		String PROV = "http://www.w3.org/ns/prov#";
		model.setNsPrefix("prov", PROV);
		while (length > 0) {
			for (String entityId : entityIds) {
				Resource entity = model.createResource(entityId);
				entity.addProperty(RDF.type, model.createResource(PROV + "Entity"));
				String query;

				// String attributedQuery = "SELECT ?s ?o WHERE {?s
				// <http://www.w3.org/ns/prov#wasAttributedTo> ?o}";
				// String associatedQuery = "SELECT ?s ?o WHERE {?s
				// <http://www.w3.org/ns/prov#wasAssociatedWith> ?o}";

				query = String.format(
						"SELECT ?activity WHERE {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity}",
						entityId);
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
						entityId);
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
						"SELECT ?entity ?activity ?sourceEntity WHERE {?entity <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> <%s>. ?entity  <http://www.w3.org/ns/prov#wasDerivedFrom> <%s>. ?entity <http://www.w3.org/ns/prov#wasDerivedFrom> ?sourceEntity. ?activity <http://www.w3.org/ns/prov#used> ?sourceEntity. }",
						entityId, entityId);
				try (QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI, query)) {
					ResultSet results = q.execSelect();
					while (results.hasNext()) {
						QuerySolution soln = results.nextSolution();
						Resource solnEntity = model.createResource(soln.get("entity").toString());
						solnEntity.addProperty(model.createProperty(PROV + "wasGeneratedBy"), soln.getResource("activity"));
						solnEntity.addProperty(model.createProperty(PROV + "wasDerivedFrom"), entity);
						solnEntity.addProperty(RDF.type, model.createResource(PROV + "Entity"));
						entityIds.add(soln.get("entity").toString());						
						Resource solnSourceEntity = model.createResource(soln.get("sourceEntity").toString());
						model.createResource(soln.getResource("activity").toString()).addProperty(model.createProperty(PROV + "used"), solnSourceEntity);
						model.createResource(soln.getResource("entity").toString()).addProperty(model.createProperty(PROV + "wasDerivedFrom"), solnSourceEntity);
						solnSourceEntity.addProperty(RDF.type, model.createResource(PROV + "Entity"));
						entityIds.add(soln.get("sourceEntity").toString());
					}
				}

				query = String.format(
						"SELECT ?entity ?activity WHERE {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> ?entity. <%s>  <http://www.w3.org/ns/prov#wasDerivedFrom> ?entity.}",
						entityId, entityId);
				try (QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI, query)) {
					ResultSet results = q.execSelect();
					while (results.hasNext()) {
						QuerySolution soln = results.nextSolution();
						// System.out.println(soln.getResource("activity"));
						Resource s = model.createResource(soln.get("activity").toString());
						Resource t = model.createResource(soln.get("entity").toString());
						s.addProperty(model.createProperty(PROV + "used"), t);
						t.addProperty(RDF.type, model.createResource(PROV + "Entity"));
						entityIds.add(soln.get("entity").toString());
					}
				}
			}
			length -= 1;
		}
		return model;
	}

	public static Document prov2mxGraph(Model model, String serviceURI) throws IOException {
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
		layer.addContent(new Element("mxCell")
			.setAttribute("parent", "0"));

		root.addContent(diagram).addContent(layer);

		try (QueryExecution dataQ = QueryExecutionFactory.create(
				"SELECT ?entity WHERE {?entity a <http://www.w3.org/ns/prov#Entity>}", 
				model)) {
			ResultSet dataResults = dataQ.execSelect();

			while (dataResults.hasNext()) {
				QuerySolution soln = dataResults.nextSolution();
				String entityId = soln.get("entity").toString();
				// nameLength determines node extent in visualization 
				int nameLength = entityId.length() * 6 + 10;
				Element dataDMP = new Element("DataDMP")
					.setAttribute("label", entityId)
					.setAttribute("id", entityId)
					.addContent(new Element("mxCell")
						.setAttribute("style", "dataDMP")
						.setAttribute("parent", "1")
						.setAttribute("vertex", "1")
						.addContent(new Element("mxGeometry")
							.setAttribute("x", "0").setAttribute("y", "0")
							.setAttribute("width", String.valueOf(nameLength))
							.setAttribute("height", "40")
							.setAttribute("as", "geometry")));
				root.addContent(dataDMP);
			}
		}

		try (QueryExecution activityQ = QueryExecutionFactory.create(
				"SELECT ?activity WHERE {?activity a <http://www.w3.org/ns/prov#Activity>}",
				model)) {
			ResultSet activityResults = activityQ.execSelect();
			while (activityResults.hasNext()) {
				QuerySolution soln = activityResults.nextSolution();
				String activityId = soln.get("activity").toString();

				int nameLength = activityId.length() * 7 + 10;
				Element processDMP = new Element("ProcessDMP")
					.setAttribute("label", activityId)
					.setAttribute("id", activityId)
					.addContent(new Element("mxCell")
						.setAttribute("style", "processDMP")
						.setAttribute("vertex", "1")
						.setAttribute("parent", "1")
						.addContent(new Element("mxGeometry")
							.setAttribute("x", "100")
							.setAttribute("y", "0")
							.setAttribute("width", String.valueOf(nameLength))
							.setAttribute("height", "40")
							.setAttribute("as", "geometry")));
				root.addContent(processDMP);
			}
		}

		try (QueryExecution agentQ = QueryExecutionFactory.create(
			"SELECT ?agent WHERE {?agent a <http://www.w3.org/ns/prov#Agent>}", 
				model)) {
			ResultSet agentResults = agentQ.execSelect();
			while (agentResults.hasNext()) {
				QuerySolution soln = agentResults.nextSolution();
				String agentId = soln.get("agent").toString();
				int nameLength = agentId.length() * 5 + 5;
				Element agentDMP = new Element("AgentDMP")
					.setAttribute("label", agentId)
					.setAttribute("id", agentId)
					.addContent(new Element("mxCell")
						.setAttribute("style", "agentDMP")
						.setAttribute("vertex", "1")
						.setAttribute("parent", "1")
						.addContent(new Element("mxGeometry")
							.setAttribute("x", "100")
							.setAttribute("y", "0")
							.setAttribute("width", String.valueOf(nameLength))
							.setAttribute("height", "40")
							.setAttribute("as", "geometry")));
				root.addContent(agentDMP);
			}
		}

		try (QueryExecution wasGeneratedByQ = QueryExecutionFactory.create(
				"SELECT ?entity ?activity WHERE {?entity <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity}", 
				model)) {
			ResultSet wasGeneratedByResults = wasGeneratedByQ.execSelect();
			int id = 2;
			while (wasGeneratedByResults.hasNext()) {
				QuerySolution soln = wasGeneratedByResults.nextSolution();

				String entityId = soln.get("entity").toString();
				String activityId = soln.get("activity").toString();

				Element connector = new Element("Connector")
					// .setAttribute("label", "wasGeneratedBy")
					.setAttribute("id", String.valueOf(id))
					.addContent(new Element("mxCell")
					.setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
					.setAttribute("edge", "1")
					.setAttribute("parent", "1")
					.setAttribute("source", activityId)
					.setAttribute("target", entityId)
						.addContent(new Element("mxGeometry")
							.setAttribute("relative", "1")
							.setAttribute("as", "geometry")
							.addContent(new Element("Array")
								.setAttribute("as", "geometry")
								.addContent(new Element("mxPoint")
									.setAttribute("x", "0")
									.setAttribute("y", "0"))
									.addContent(new Element("mxPoint")
										.setAttribute("x", "0")
										.setAttribute("y", "0")))));
				root.addContent(connector);
				id++;
			}
		}

		try (QueryExecution usedQ = QueryExecutionFactory.create(
				"SELECT ?activity ?entity WHERE {?activity <http://www.w3.org/ns/prov#used> ?entity}", 
				model)) {
			ResultSet usedResults = usedQ.execSelect();
			int id = 2;
			while (usedResults.hasNext()) {
				QuerySolution soln = usedResults.nextSolution();

				String activityId = soln.get("activity").toString();
				String entityId = soln.get("entity").toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "used")
						.setAttribute("id", String.valueOf(id) + "b")
						.addContent(new Element("mxCell")
							.setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
							.setAttribute("edge", "1")
							.setAttribute("parent", "1")
							.setAttribute("source", entityId)
							.setAttribute("target", activityId)
							.addContent(new Element("mxGeometry")
								.setAttribute("relative", "1")
								.setAttribute("as", "geometry")
								.addContent(new Element("Array")
									.setAttribute("as", "geometry")
									.addContent(new Element("mxPoint")
										.setAttribute("x", "0")
										.setAttribute("y", "0"))
										.addContent(new Element("mxPoint")
											.setAttribute("x", "0")
											.setAttribute("y", "0")))));
				root.addContent(connector);
				id++;
			}
		}

		try (QueryExecution associatedQ = QueryExecutionFactory.create(
				"SELECT ?activity ?agent WHERE {?activity <http://www.w3.org/ns/prov#wasAssociatedWith> ?agent}", 
				model)) {
			ResultSet associatedResults = associatedQ.execSelect();
			int id = 2;
			while (associatedResults.hasNext()) {
				QuerySolution soln = associatedResults.nextSolution();

				String activityId = soln.get("activity").toString();
				String agentId = soln.get("agent").toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "wasAssociatedWith")
						.setAttribute("id", String.valueOf(id) + "c")
						.addContent(new Element("mxCell")
							.setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
							.setAttribute("edge", "1")
							.setAttribute("parent", "1")
							.setAttribute("source", activityId)
							.setAttribute("target", agentId)
							.addContent(new Element("mxGeometry")
								.setAttribute("relative", "1")
								.setAttribute("as", "geometry")
								.addContent(new Element("Array")
									.setAttribute("as", "geometry")
									.addContent(new Element("mxPoint")
										.setAttribute("x", "0")
										.setAttribute("y", "0"))
										.addContent(new Element("mxPoint")
											.setAttribute("x", "0")
											.setAttribute("y", "0")))));
				root.addContent(connector);
				id++;
			}
		}

		try (QueryExecution attributedQ = QueryExecutionFactory.create(
				"SELECT ?entity ?agent WHERE {?entity <http://www.w3.org/ns/prov#wasAttributedTo> ?agent}", 
				model)) {
			ResultSet attributedResults = attributedQ.execSelect();
			int id = 2;
			while (attributedResults.hasNext()) {
				QuerySolution soln = attributedResults.nextSolution();

				String entityId = soln.get("entity").toString();
				String agentId = soln.get("agent").toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "wasAttributedTo")
						.setAttribute("id", String.valueOf(id) + "d")
						.addContent(new Element("mxCell")
							.setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
							.setAttribute("edge", "1")
							.setAttribute("parent", "1")
							.setAttribute("source", entityId)
							.setAttribute("target", agentId)
							.addContent(new Element("mxGeometry")
								.setAttribute("relative", "1")
								.setAttribute("as", "geometry")
								.addContent(new Element("Array")
									.setAttribute("as", "geometry")
									.addContent(new Element("mxPoint")
										.setAttribute("x", "0")
										.setAttribute("y", "0"))
										.addContent(new Element("mxPoint")
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
		Model model;
		Boolean destroySession = Boolean.parseBoolean(request.getParameter("destroySession"));

		HttpSession session = request.getSession();
		if (destroySession) {
			session.invalidate();
			session = request.getSession();
		}
		System.out.println();
		System.out.println("Session:");
		System.out.println(session.getId());
		if (session.getAttribute("model") != null) {
			System.out.println("load existing model");
			model = (Model) session.getAttribute("model");
		} else {
			System.out.println("create default model");
			model = ModelFactory.createDefaultModel();
		}
		try {
			String entityId = request.getParameter("entityId");
			String endpoint = request.getParameter("endpoint");
			int pathLen = Integer.parseInt(request.getParameter("pathLen"));
			Set<String> entities = new LinkedHashSet();
			entities.add(entityId);
			model = constructSubGraph(model, endpoint, entities, pathLen);
			session.setAttribute("model", model);
			Document doc = prov2mxGraph(model, endpoint);
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
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}
}
