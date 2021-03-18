import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URLDecoder;

@WebServlet(name = "Prov2Graph")

public class Prov2Graph extends HttpServlet {
    /**
     *
     */
    private static final long serialVersionUID = 1L;


    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getContentLength() < Constants.MAX_REQUEST_SIZE) {
            String filename = request.getParameter("filename");
            String xml = request.getParameter("xml");

            if (filename == null) {
                filename = "export";
            }

            if (xml != null && xml.length() > 0) {
                String format = request.getParameter("format");

                if (format == null) {
                    format = "xml";
                }

                if (!filename.toLowerCase().endsWith("." + format)) {
                    filename += "." + format;
                }

                // Decoding is optional (no plain text values allowed)
                if (xml != null && xml.startsWith("%3C")) {
                    xml = URLDecoder.decode(xml, "UTF-8");
                }

                try {
                    Document doc = new SAXBuilder().build(new StringReader(xml));
                    
                    // Source xmlFile = new JDOMSource(doc);
                    // JDOMResult provResult = new JDOMResult();
                    // Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(
                    //         String.valueOf(getClass().getClassLoader().getResource("Prov2Graph.xsl"))));
                    // transformer.transform(xmlFile, provResult);


                    
                    response.setContentType("text/plain");
                    response.setHeader("Content-Disposition",
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename);
                    response.setStatus(HttpServletResponse.SC_OK);
                    OutputStream out = response.getOutputStream();
                    new XMLOutputter(Format.getPrettyFormat()).output(doc, out);
                    // new XMLOutputter(Format.getPrettyFormat()).output(provResult.getDocument(), out);
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
