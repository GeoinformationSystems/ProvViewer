import java.io.*;
import java.net.URLDecoder;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import javax.servlet.annotation.*;
/* The Java file upload Servlet example */

@WebServlet(name = "QueryGraph")
// @MultipartConfig(fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
// maxFileSize = 1024 * 1024 * 10, // 10 MB
// maxRequestSize = 1024 * 1024 * 100 // 100 MB
// )
public class QueryGraph extends HttpServlet {

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String query = URLDecoder.decode(request.getParameter("query"), "UTF-8");
        HttpSession session = request.getSession();
        Model uploadModel = (Model) session.getAttribute("uploadModel");

        try (QueryExecution nameQ = QueryExecutionFactory.create(query, uploadModel)) {
            ResultSet resultSet = nameQ.execSelect();
            OutputStream out = response.getOutputStream();
            ResultSetFormatter.outputAsJSON(out, resultSet);
            out.flush();
            out.close();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}