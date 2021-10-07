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
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

@WebServlet(name = "Prov2Graph")

public class Prov2Graph extends HttpServlet {
	/**
	 * Class that queries a triplestore for provenance information according to
	 * PROV-O and generates a visualizable mxGraph object. Method constructSubGraph:
	 * Takes a list of RDF resources as input and queries a triplestore for
	 * provenance information that are related to those resources. From results of
	 * this query, a local RDF graph (with apache-jena-lib) object is constructed.
	 * This newly constructed graph object only contains provenance information.
	 * Multiple calls of the constructSubGraph() method from within the same web
	 * session, do not reset the aforementioned local RDF graph object, but add the
	 * queried information to the object. This way the graph can be expanded
	 * iteratively. The parameter length is experimental; tested with length = 1. It
	 * determines the number of provenance steps that is added to the local RDF
	 * graph object for each resource. I.e., if length = 1; for each resource in the
	 * provenance graph, only the direct neighbors (identified by 'wasDerivedFrom')
	 * of the resource are added to the subgraph. Method prov2mxGraph: Takes the
	 * local RDF subgraph and converts it into an mxGraph object. Furthermore, for
	 * each resource in the local RDF subgraph, the original RDF graph is queried
	 * for information to enrich the mxGraph object with metadata (currently common
	 * RDF labels [rdfs:label, skos:prefLabel, dct:title]). The methods are called
	 * in doPost and the resulting mxGraph object is returned to the client. Every
	 * Method in Prov2Graph is polymorph; and takes either an apache jena RDF model
	 * or a string that points to a triplestore as input. The apache jena model
	 * (@param inModel) is used for RDF file uploads, the triplestore URI (@param
	 * serviceURI) is used when querying a triplestore directly.
	 * 
	 **/
	private static final long serialVersionUID = 1L;

	private static String getNodeName(String nodeId, String serviceURI) {
		String nodeName = null;
		String query = String.format(
				"SELECT ?nodeName WHERE {OPTIONAL {<%s> <http://purl.org/dc/terms/title> ?nodeName} OPTIONAL {<%s> <http://www.w3.org/2004/02/skos/core#prefLabel> ?nodeName} OPTIONAL {<%s> <http://www.w3.org/2000/01/rdf-schema#label> ?nodeName}}",
				nodeId, nodeId, nodeId);
		try (QueryExecution nameQ = QueryExecutionFactory.sparqlService(serviceURI, query)) {
			ResultSet nameResults = nameQ.execSelect();
			while (nameResults.hasNext()) {
				QuerySolution solution = nameResults.nextSolution();
				if (solution.get("nodeName") != null) {
					nodeName = solution.get("nodeName").toString();
				}
			}
		}
		return nodeName;
	}

	private static String getNodeName(String nodeId, Model inModel) {
		String nodeName = null;
		String query = String.format(
				"SELECT ?nodeName WHERE {OPTIONAL {<%s> <http://purl.org/dc/terms/title> ?nodeName} OPTIONAL {<%s> <http://www.w3.org/2004/02/skos/core#prefLabel> ?nodeName} OPTIONAL {<%s> <http://www.w3.org/2000/01/rdf-schema#label> ?nodeName}}",
				nodeId, nodeId, nodeId);
		try (QueryExecution nameQ = QueryExecutionFactory.create(query, inModel)) {
			ResultSet nameResults = nameQ.execSelect();
			while (nameResults.hasNext()) {
				QuerySolution solution = nameResults.nextSolution();
				if (solution.get("nodeName") != null) {
					nodeName = solution.get("nodeName").toString();
				}
			}
		}
		return nodeName;
	}

	public static Model constructSubGraph(Model model, String serviceURI, Set<String> entityIds, int length)
			throws IOException {

		String PROV = "http://www.w3.org/ns/prov#";
		model.setNsPrefix("prov", PROV);
		while (length > 0) {
			for (String entityId : entityIds) {
				String query;
				ResultSet results;

				Resource entity = model.createResource(entityId);
				entity.addProperty(RDF.type, model.createResource(PROV + "Entity"));

				// query the 'future' of @entity
				query = String.format(
						"SELECT DISTINCT ?futureEntity WHERE {{?futureEntity <http://www.w3.org/ns/prov#wasDerivedFrom> <%s>.} UNION {?futureEntity <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> <%s>.}}",
						entityId, entityId);
				results = QueryExecutionFactory.sparqlService(serviceURI, query).execSelect();
				while (results.hasNext()) {
					String futureEntityId = results.nextSolution().get("futureEntity").toString();
					Resource futureEntity = model.createResource(futureEntityId)
							.addProperty(RDF.type, model.createResource(PROV + "Entity"))
							.addProperty(model.createProperty(PROV + "wasDerivedFrom"), entity);
					// query for an @activity that connects @futureEntity and @entity
					String activityQuery = String.format(
							"SELECT ?activity WHERE {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> <%s>.}",
							futureEntityId, entityId);
					ResultSet activityResults = QueryExecutionFactory.sparqlService(serviceURI, activityQuery)
							.execSelect();
					// if there is no @activity found, set the @activity up as a Blank Node.

					if (activityResults.hasNext()) {
						// System.out.println("add existing activity");
						while (activityResults.hasNext()) {
							String activityId = activityResults.nextSolution().get("activity").toString();
							Resource activity = model.createResource(activityId)
									.addProperty(RDF.type, model.createResource(PROV + "Activity"))
									.addProperty(model.createProperty(PROV + "used"), entity);
							futureEntity.addProperty(model.createProperty(PROV + "wasGeneratedBy"), activity);
						}
					} else {
						// check the local model if there was already a blank node constructed
						ResultSet bNodeRes = QueryExecutionFactory.create(activityQuery, model).execSelect();
						if (!(bNodeRes.hasNext())) {
							// System.out.println("make blank node");
							Resource activity = model.createResource()
									.addProperty(RDF.type, model.createResource(PROV + "Activity"))
									.addProperty(RDFS.label, model.createLiteral("undefined"))
									.addProperty(model.createProperty(PROV + "used"), entity);
							futureEntity.addProperty(model.createProperty(PROV + "wasGeneratedBy"), activity);
						}

					}
				}

				// query the 'past' of @entity
				query = String.format(
						"SELECT DISTINCT ?pastEntity WHERE {{<%s> <http://www.w3.org/ns/prov#wasDerivedFrom> ?pastEntity.} UNION {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> ?pastEntity.}}",
						entityId, entityId);
				results = QueryExecutionFactory.sparqlService(serviceURI, query).execSelect();
				while (results.hasNext()) {
					String pastEntityId = results.nextSolution().get("pastEntity").toString();
					Resource pastEntity = model.createResource(pastEntityId).addProperty(RDF.type,
							model.createResource(PROV + "Entity"));
					entity.addProperty(model.createProperty(PROV + "wasDerivedFrom"), pastEntity);
					// query for an @activity that connects @futureEntity and @entity
					String activityQuery = String.format(
							"SELECT ?activity WHERE {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> <%s>.}",
							entityId, pastEntityId);
					ResultSet activityResults = QueryExecutionFactory.sparqlService(serviceURI, activityQuery)
							.execSelect();
					// if there is no @activity found, set the @activity up as a Blank Node.

					if (activityResults.hasNext()) {
						// System.out.println("add existing activity");
						while (activityResults.hasNext()) {
							String activityId = activityResults.nextSolution().get("activity").toString();
							Resource activity = model.createResource(activityId)
									.addProperty(RDF.type, model.createResource(PROV + "Activity"))
									.addProperty(model.createProperty(PROV + "used"), pastEntity);
							entity.addProperty(model.createProperty(PROV + "wasGeneratedBy"), activity);
						}
					} else {
						// check the local model if there was already a blank node constructed
						// only build new blank node if @entity does not have any 'wasGeneratedBy'
						// relation (this assumes that a given entity can not be generated by different
						// activities.)
						String wasGeneratedByQuery = String.format(
								"SELECT ?activity WHERE {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity.}",
								entityId);
						ResultSet bNodeRes = QueryExecutionFactory.create(wasGeneratedByQuery, model).execSelect();
						if (!(bNodeRes.hasNext())) {
							// System.out.println("make blank node");
							Resource activity = model.createResource()
									.addProperty(RDF.type, model.createResource(PROV + "Activity"))
									.addProperty(model.createProperty(PROV + "used"), pastEntity);
							entity.addProperty(model.createProperty(PROV + "wasGeneratedBy"), activity);
						} else {
							while (bNodeRes.hasNext()) {
								bNodeRes.nextSolution().get("activity").asResource()
										.addProperty(model.createProperty(PROV + "used"), pastEntity);
							}
						}

					}
				}

			}
			length -= 1;
		}
		return model;
	}

	public static Model constructSubGraph(Model model, Model inModel, Set<String> entityIds, int length)
			throws IOException {

		String PROV = "http://www.w3.org/ns/prov#";
		model.setNsPrefix("prov", PROV);
		while (length > 0) {
			for (String entityId : entityIds) {
				String query;
				ResultSet results;

				Resource entity = model.createResource(entityId);
				entity.addProperty(RDF.type, model.createResource(PROV + "Entity"));

				// query the 'future' of @entity
				query = String.format(
						"SELECT DISTINCT ?futureEntity WHERE {{?futureEntity <http://www.w3.org/ns/prov#wasDerivedFrom> <%s>.} UNION {?futureEntity <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> <%s>.}}",
						entityId, entityId);
				results = QueryExecutionFactory.create(query, inModel).execSelect();
				while (results.hasNext()) {
					String futureEntityId = results.nextSolution().get("futureEntity").toString();
					Resource futureEntity = model.createResource(futureEntityId)
							.addProperty(RDF.type, model.createResource(PROV + "Entity"))
							.addProperty(model.createProperty(PROV + "wasDerivedFrom"), entity);
					// query for an @activity that connects @futureEntity and @entity
					String activityQuery = String.format(
							"SELECT ?activity WHERE {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> <%s>.}",
							futureEntityId, entityId);
					ResultSet activityResults = QueryExecutionFactory.create(activityQuery, inModel).execSelect();
					// if there is no @activity found, set the @activity up as a Blank Node.

					if (activityResults.hasNext()) {
						// System.out.println("add existing activity");
						while (activityResults.hasNext()) {
							String activityId = activityResults.nextSolution().get("activity").toString();
							Resource activity = model.createResource(activityId)
									.addProperty(RDF.type, model.createResource(PROV + "Activity"))
									.addProperty(model.createProperty(PROV + "used"), entity);
							futureEntity.addProperty(model.createProperty(PROV + "wasGeneratedBy"), activity);
						}
					} else {
						// check the local model if there was already a blank node constructed
						ResultSet bNodeRes = QueryExecutionFactory.create(activityQuery, model).execSelect();
						if (!(bNodeRes.hasNext())) {
							// System.out.println("make blank node");
							Resource activity = model.createResource()
									.addProperty(RDF.type, model.createResource(PROV + "Activity"))
									.addProperty(RDFS.label, model.createLiteral("undefined"))
									.addProperty(model.createProperty(PROV + "used"), entity);
							futureEntity.addProperty(model.createProperty(PROV + "wasGeneratedBy"), activity);
						}

					}
				}

				// query the 'past' of @entity
				query = String.format(
						"SELECT DISTINCT ?pastEntity WHERE {{<%s> <http://www.w3.org/ns/prov#wasDerivedFrom> ?pastEntity.} UNION {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> ?pastEntity.}}",
						entityId, entityId);
				results = QueryExecutionFactory.create(query, inModel).execSelect();
				while (results.hasNext()) {
					String pastEntityId = results.nextSolution().get("pastEntity").toString();
					Resource pastEntity = model.createResource(pastEntityId).addProperty(RDF.type,
							model.createResource(PROV + "Entity"));
					entity.addProperty(model.createProperty(PROV + "wasDerivedFrom"), pastEntity);
					// query for an @activity that connects @futureEntity and @entity
					String activityQuery = String.format(
							"SELECT ?activity WHERE {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> <%s>.}",
							entityId, pastEntityId);
					ResultSet activityResults = QueryExecutionFactory.create(activityQuery, inModel).execSelect();
					// if there is no @activity found, set the @activity up as a Blank Node.

					if (activityResults.hasNext()) {
						// System.out.println("add existing activity");
						while (activityResults.hasNext()) {
							String activityId = activityResults.nextSolution().get("activity").toString();
							Resource activity = model.createResource(activityId)
									.addProperty(RDF.type, model.createResource(PROV + "Activity"))
									.addProperty(model.createProperty(PROV + "used"), pastEntity);
							entity.addProperty(model.createProperty(PROV + "wasGeneratedBy"), activity);
						}
					} else {
						// check the local model if there was already a blank node constructed
						// only build new blank node if @entity does not have any 'wasGeneratedBy'
						// relation (this assumes that a given entity can not be generated by different
						// activities.)
						String wasGeneratedByQuery = String.format(
								"SELECT ?activity WHERE {<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity.}",
								entityId);
						ResultSet bNodeRes = QueryExecutionFactory.create(wasGeneratedByQuery, model).execSelect();
						if (!(bNodeRes.hasNext())) {
							// System.out.println("make blank node");
							Resource activity = model.createResource()
									.addProperty(RDF.type, model.createResource(PROV + "Activity"))
									.addProperty(model.createProperty(PROV + "used"), pastEntity);
							entity.addProperty(model.createProperty(PROV + "wasGeneratedBy"), activity);
						} else {
							while (bNodeRes.hasNext()) {
								bNodeRes.nextSolution().get("activity").asResource()
										.addProperty(model.createProperty(PROV + "used"), pastEntity);
							}
						}

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

		Element diagram = new Element("Diagram").setAttribute("label", "My Diagram")
				.setAttribute("href", "http://www.jgraph.com/").setAttribute("id", "0");
		diagram.addContent(new Element("mxCell"));

		Element layer = new Element("Layer").setAttribute("label", "Default Layer").setAttribute("id", "1");
		layer.addContent(new Element("mxCell").setAttribute("parent", "0"));

		root.addContent(diagram).addContent(layer);

		try (QueryExecution dataQ = QueryExecutionFactory
				.create("SELECT ?entity WHERE {?entity a <http://www.w3.org/ns/prov#Entity>}", model)) {
			ResultSet dataResults = dataQ.execSelect();

			while (dataResults.hasNext()) {
				QuerySolution solution = dataResults.nextSolution();
				String entityId = solution.get("entity").toString();
				// nameLength determines node extent in visualization
				String entityName = getNodeName(entityId, serviceURI);
				if (entityName == null) {
					entityName = entityId;
				}
				int nameLength = entityName.length() * 6 + 10;
				// count number of entity-neighbors in local graph
				String countQuery = String.format(
						"SELECT DISTINCT ?entity WHERE {{<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> ?entity} UNION {?entity <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> <%s>.}}",
						entityId, entityId);
				// System.out.println(countQuery);
				ResultSet countRes = QueryExecutionFactory.create(countQuery, model).execSelect();
				Set<String> neighbors = new LinkedHashSet();
				// System.out.println(entityName);
				while (countRes.hasNext()) {
					QuerySolution countSolution = countRes.nextSolution();
					neighbors.add(countSolution.get("entity").toString());
				}
				// System.out.println(neighbors.size());

				Element dataDMP = new Element("DataDMP").setAttribute("provConcept", "entity")
						.setAttribute("entityNeighbors", String.valueOf(neighbors.size()))
						.setAttribute("label", entityName).setAttribute("id", entityId)
						.addContent(new Element("mxCell").setAttribute("style", "dataDMP").setAttribute("parent", "1")
								.setAttribute("vertex", "1")
								.addContent(new Element("mxGeometry").setAttribute("x", "0").setAttribute("y", "0")
										.setAttribute("width", String.valueOf(nameLength)).setAttribute("height", "40")
										.setAttribute("as", "geometry")));
				root.addContent(dataDMP);
			}
		}

		try (QueryExecution activityQ = QueryExecutionFactory
				.create("SELECT ?activity WHERE {?activity a <http://www.w3.org/ns/prov#Activity>}", model)) {
			ResultSet activityResults = activityQ.execSelect();
			while (activityResults.hasNext()) {
				QuerySolution solution = activityResults.nextSolution();
				Resource activity = solution.getResource("activity");
				String activityId = activity.toString();
				String activityName = getNodeName(activityId, serviceURI);

				if (activityName == null) {
					// if node is blank node, set label "undefined"
					if (activity.asNode().getBlankNodeId() != null) {
						activityName = "undefined";
					} else {
						activityName = activityId;
					}

				}
				int nameLength = activityName.length() * 7 + 10;
				Element processDMP = new Element("ProcessDMP").setAttribute("provConcept", "activity")
						.setAttribute("label", activityName).setAttribute("id", activityId)
						.addContent(new Element("mxCell").setAttribute("style", "processDMP")
								.setAttribute("vertex", "1").setAttribute("parent", "1")
								.addContent(new Element("mxGeometry").setAttribute("x", "100").setAttribute("y", "0")
										.setAttribute("width", String.valueOf(nameLength)).setAttribute("height", "40")
										.setAttribute("as", "geometry")));
				root.addContent(processDMP);
			}
		}

		try (QueryExecution agentQ = QueryExecutionFactory
				.create("SELECT ?agent WHERE {?agent a <http://www.w3.org/ns/prov#Agent>}", model)) {
			ResultSet agentResults = agentQ.execSelect();
			while (agentResults.hasNext()) {
				QuerySolution solution = agentResults.nextSolution();
				String agentId = solution.get("agent").toString();
				String agentName = getNodeName(agentId, serviceURI);
				if (agentName == null) {
					agentName = agentId;
				}
				int nameLength = agentName.length() * 5 + 5;
				Element agentDMP = new Element("AgentDMP").setAttribute("provConcept", "agent")
						.setAttribute("label", agentName).setAttribute("id", agentId)
						.addContent(new Element("mxCell").setAttribute("style", "agentDMP").setAttribute("vertex", "1")
								.setAttribute("parent", "1")
								.addContent(new Element("mxGeometry").setAttribute("x", "100").setAttribute("y", "0")
										.setAttribute("width", String.valueOf(nameLength)).setAttribute("height", "40")
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
				QuerySolution solution = wasGeneratedByResults.nextSolution();

				String entityId = solution.get("entity").toString();
				String activityId = solution.get("activity").toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "wasGeneratedBy")
						.setAttribute("id", String.valueOf(id))
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1")
								.setAttribute("source", activityId).setAttribute("target", entityId)
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

		try (QueryExecution usedQ = QueryExecutionFactory
				.create("SELECT ?activity ?entity WHERE {?activity <http://www.w3.org/ns/prov#used> ?entity}", model)) {
			ResultSet usedResults = usedQ.execSelect();
			int id = 2;
			while (usedResults.hasNext()) {
				QuerySolution solution = usedResults.nextSolution();

				String activityId = solution.get("activity").toString();
				String entityId = solution.get("entity").toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "used")
						.setAttribute("id", String.valueOf(id) + "b")
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source", entityId)
								.setAttribute("target", activityId)
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

		try (QueryExecution associatedQ = QueryExecutionFactory.create(
				"SELECT ?activity ?agent WHERE {?activity <http://www.w3.org/ns/prov#wasAssociatedWith> ?agent}",
				model)) {
			ResultSet associatedResults = associatedQ.execSelect();
			int id = 2;
			while (associatedResults.hasNext()) {
				QuerySolution solution = associatedResults.nextSolution();

				String activityId = solution.get("activity").toString();
				String agentId = solution.get("agent").toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "wasAssociatedWith")
						.setAttribute("id", String.valueOf(id) + "c")
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1")
								.setAttribute("source", activityId).setAttribute("target", agentId)
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

		try (QueryExecution attributedQ = QueryExecutionFactory.create(
				"SELECT ?entity ?agent WHERE {?entity <http://www.w3.org/ns/prov#wasAttributedTo> ?agent}", model)) {
			ResultSet attributedResults = attributedQ.execSelect();
			int id = 2;
			while (attributedResults.hasNext()) {
				QuerySolution solution = attributedResults.nextSolution();

				String entityId = solution.get("entity").toString();
				String agentId = solution.get("agent").toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "wasAttributedTo")
						.setAttribute("id", String.valueOf(id) + "d")
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source", entityId)
								.setAttribute("target", agentId)
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

	public static Document prov2mxGraph(Model model, Model inModel) throws IOException {
		Document document = new Document();
		Element mxGraphModel = new Element("mxGraphModel");

		Element root = new Element("root");

		Element diagram = new Element("Diagram").setAttribute("label", "My Diagram")
				.setAttribute("href", "http://www.jgraph.com/").setAttribute("id", "0");
		diagram.addContent(new Element("mxCell"));

		Element layer = new Element("Layer").setAttribute("label", "Default Layer").setAttribute("id", "1");
		layer.addContent(new Element("mxCell").setAttribute("parent", "0"));

		root.addContent(diagram).addContent(layer);

		try (QueryExecution dataQ = QueryExecutionFactory
				.create("SELECT ?entity WHERE {?entity a <http://www.w3.org/ns/prov#Entity>}", model)) {
			ResultSet dataResults = dataQ.execSelect();

			while (dataResults.hasNext()) {
				QuerySolution solution = dataResults.nextSolution();
				String entityId = solution.get("entity").toString();
				// nameLength determines node extent in visualization
				String entityName = getNodeName(entityId, inModel);
				if (entityName == null) {
					entityName = entityId;
				}
				int nameLength = entityName.length() * 6 + 10;
				// count number of entity-neighbors in local graph
				String countQuery = String.format(
						"SELECT DISTINCT ?entity WHERE {{<%s> <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> ?entity} UNION {?entity <http://www.w3.org/ns/prov#wasGeneratedBy> ?activity. ?activity <http://www.w3.org/ns/prov#used> <%s>.}}",
						entityId, entityId);
				// System.out.println(countQuery);
				ResultSet countRes = QueryExecutionFactory.create(countQuery, model).execSelect();
				Set<String> neighbors = new LinkedHashSet();
				// System.out.println(entityName);
				while (countRes.hasNext()) {
					QuerySolution countSolution = countRes.nextSolution();
					neighbors.add(countSolution.get("entity").toString());
				}
				// System.out.println(neighbors.size());

				Element dataDMP = new Element("DataDMP").setAttribute("provConcept", "entity")
						.setAttribute("entityNeighbors", String.valueOf(neighbors.size()))
						.setAttribute("label", entityName).setAttribute("id", entityId)
						.addContent(new Element("mxCell").setAttribute("style", "dataDMP").setAttribute("parent", "1")
								.setAttribute("vertex", "1")
								.addContent(new Element("mxGeometry").setAttribute("x", "0").setAttribute("y", "0")
										.setAttribute("width", String.valueOf(nameLength)).setAttribute("height", "40")
										.setAttribute("as", "geometry")));
				root.addContent(dataDMP);
			}
		}

		try (QueryExecution activityQ = QueryExecutionFactory
				.create("SELECT ?activity WHERE {?activity a <http://www.w3.org/ns/prov#Activity>}", model)) {
			ResultSet activityResults = activityQ.execSelect();
			while (activityResults.hasNext()) {
				QuerySolution solution = activityResults.nextSolution();
				Resource activity = solution.getResource("activity");
				String activityId = activity.toString();
				String activityName = getNodeName(activityId, inModel);

				if (activityName == null) {
					// if node is blank node, set label "undefined"
					try {
						activity.asNode().getBlankNodeId();
						activityName = "undefined";
					} catch (java.lang.UnsupportedOperationException unsupportedEx) {
						// System.out.println("Node is no BlankNode");
						activityName = activityId;
					}
					// if (activity.asNode().getBlankNodeId() != null) {
					// activityName = "undefined";
					// } else {
					// activityName = activityId;
					// }

				}
				int nameLength = activityName.length() * 7 + 10;
				Element processDMP = new Element("ProcessDMP").setAttribute("provConcept", "activity")
						.setAttribute("label", activityName).setAttribute("id", activityId)
						.addContent(new Element("mxCell").setAttribute("style", "processDMP")
								.setAttribute("vertex", "1").setAttribute("parent", "1")
								.addContent(new Element("mxGeometry").setAttribute("x", "100").setAttribute("y", "0")
										.setAttribute("width", String.valueOf(nameLength)).setAttribute("height", "40")
										.setAttribute("as", "geometry")));
				root.addContent(processDMP);
			}
		}

		try (QueryExecution agentQ = QueryExecutionFactory
				.create("SELECT ?agent WHERE {?agent a <http://www.w3.org/ns/prov#Agent>}", model)) {
			ResultSet agentResults = agentQ.execSelect();
			while (agentResults.hasNext()) {
				QuerySolution solution = agentResults.nextSolution();
				String agentId = solution.get("agent").toString();
				String agentName = getNodeName(agentId, inModel);
				if (agentName == null) {
					agentName = agentId;
				}
				int nameLength = agentName.length() * 5 + 5;
				Element agentDMP = new Element("AgentDMP").setAttribute("provConcept", "agent")
						.setAttribute("label", agentName).setAttribute("id", agentId)
						.addContent(new Element("mxCell").setAttribute("style", "agentDMP").setAttribute("vertex", "1")
								.setAttribute("parent", "1")
								.addContent(new Element("mxGeometry").setAttribute("x", "100").setAttribute("y", "0")
										.setAttribute("width", String.valueOf(nameLength)).setAttribute("height", "40")
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
				QuerySolution solution = wasGeneratedByResults.nextSolution();

				String entityId = solution.get("entity").toString();
				String activityId = solution.get("activity").toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "wasGeneratedBy")
						.setAttribute("id", String.valueOf(id))
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1")
								.setAttribute("source", activityId).setAttribute("target", entityId)
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

		try (QueryExecution usedQ = QueryExecutionFactory
				.create("SELECT ?activity ?entity WHERE {?activity <http://www.w3.org/ns/prov#used> ?entity}", model)) {
			ResultSet usedResults = usedQ.execSelect();
			int id = 2;
			while (usedResults.hasNext()) {
				QuerySolution solution = usedResults.nextSolution();

				String activityId = solution.get("activity").toString();
				String entityId = solution.get("entity").toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "used")
						.setAttribute("id", String.valueOf(id) + "b")
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source", entityId)
								.setAttribute("target", activityId)
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

		try (QueryExecution associatedQ = QueryExecutionFactory.create(
				"SELECT ?activity ?agent WHERE {?activity <http://www.w3.org/ns/prov#wasAssociatedWith> ?agent}",
				model)) {
			ResultSet associatedResults = associatedQ.execSelect();
			int id = 2;
			while (associatedResults.hasNext()) {
				QuerySolution solution = associatedResults.nextSolution();

				String activityId = solution.get("activity").toString();
				String agentId = solution.get("agent").toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "wasAssociatedWith")
						.setAttribute("id", String.valueOf(id) + "c")
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1")
								.setAttribute("source", activityId).setAttribute("target", agentId)
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

		try (QueryExecution attributedQ = QueryExecutionFactory.create(
				"SELECT ?entity ?agent WHERE {?entity <http://www.w3.org/ns/prov#wasAttributedTo> ?agent}", model)) {
			ResultSet attributedResults = attributedQ.execSelect();
			int id = 2;
			while (attributedResults.hasNext()) {
				QuerySolution solution = attributedResults.nextSolution();

				String entityId = solution.get("entity").toString();
				String agentId = solution.get("agent").toString();

				Element connector = new Element("Connector")
						// .setAttribute("label", "wasAttributedTo")
						.setAttribute("id", String.valueOf(id) + "d")
						.addContent(new Element("mxCell").setAttribute("style", "noEdgeStyle=1;orthogonal=1;")
								.setAttribute("edge", "1").setAttribute("parent", "1").setAttribute("source", entityId)
								.setAttribute("target", agentId)
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

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Model model;
		Model uploadModel;
		HttpSession session = request.getSession();

		if (session.getAttribute("uploadModel") != null) {
			uploadModel = (Model) session.getAttribute("uploadModel");
		} else {
			System.out.println("no model uploaded!");
			uploadModel = ModelFactory.createDefaultModel();
		}

		Boolean destroySession = Boolean.parseBoolean(request.getParameter("destroySession"));
		if (destroySession) {
			session.invalidate();
			session = request.getSession();
			// rescue uploaded model over session destruction
			session.setAttribute("uploadModel", uploadModel);
		}

		// destroy session if endpoint changes; not really req now
		// if (session.getAttribute("endpoint") != null) {
		// if ((String) session.getAttribute("endpoint") != (String)
		// request.getParameter("endpoint")) {
		// session.invalidate();
		// session = request.getSession();
		// }
		// }
		System.out.println();
		System.out.println("Session:");
		System.out.println(session.getId());
		if (session.getAttribute("model") != null) {
			// System.out.println("load existing model");
			model = (Model) session.getAttribute("model");
		} else {
			// System.out.println("create default model");
			model = ModelFactory.createDefaultModel();
		}

		try {
			String entityId = request.getParameter("entityId");
			String endpoint = request.getParameter("endpoint");
			int pathLen = Integer.parseInt(request.getParameter("pathLen"));
			Set<String> entities = new LinkedHashSet();
			entities.add(entityId);
			Document doc;
			if (endpoint.equals("local")) {
				model = constructSubGraph(model, uploadModel, entities, pathLen);
				session.setAttribute("model", model);
				doc = prov2mxGraph(model, uploadModel);
			} else {
				model = constructSubGraph(model, endpoint, entities, pathLen);
				session.setAttribute("model", model);
				doc = prov2mxGraph(model, endpoint);
			}

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
