import java.io.*;
import java.net.URLDecoder;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import javax.servlet.annotation.*;
/* The Java file upload Servlet example */

@WebServlet(name = "ProvOUpload")
// @MultipartConfig(fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
// maxFileSize = 1024 * 1024 * 10, // 10 MB
// maxRequestSize = 1024 * 1024 * 100 // 100 MB
// )
public class ProvOUpload extends HttpServlet {

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    String fileExtension = URLDecoder.decode(request.getParameter("fileextension"), "UTF-8");
    String fileContent = URLDecoder.decode(request.getParameter("filecontent"), "UTF-8");
    System.out.println(fileExtension);
    Model uploadModel = ModelFactory.createDefaultModel();
    switch (fileExtension) {
      case "ttl":
        uploadModel.read(IOUtils.toInputStream(fileContent, "UTF-8"), null, "TURTLE");
        break;
      case "xml":
        uploadModel.read(IOUtils.toInputStream(fileContent, "UTF-8"), null, "RDF/XML");
        break;
      case "rdf":
        uploadModel.read(IOUtils.toInputStream(fileContent, "UTF-8"), null, "RDF/XML");
        break;
      case "n3":
        uploadModel.read(IOUtils.toInputStream(fileContent, "UTF-8"), null, "N-TRIPLES");
        break;
      case "nt":
        uploadModel.read(IOUtils.toInputStream(fileContent, "UTF-8"), null, "N-TRIPLES");
        break;
      case "jsonld":
        uploadModel.read(IOUtils.toInputStream(fileContent, "UTF-8"), null, "JSON-LD");
        break;
      case "json":
        uploadModel.read(IOUtils.toInputStream(fileContent, "UTF-8"), null, "RDF/JSON");
        break;
      case "rdfjson":
        uploadModel.read(IOUtils.toInputStream(fileContent, "UTF-8"), null, "RDF/JSON");
        break;
      default:
        System.out.println("Wrong format.");
    }
    System.out.println("model size: " + uploadModel.size());
    HttpSession session = request.getSession();
    session.setAttribute("uploadModel", uploadModel);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }

}