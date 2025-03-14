package movies;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "AutocompleteServlet", urlPatterns = "/api/autocomplete")
public class AutocompleteServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String searchQuery = request.getParameter("query");
        JsonArray jsonArray = new JsonArray();

        if (searchQuery == null || searchQuery.trim().length() < 3) {
            out.write(jsonArray.toString());
            response.setStatus(200);
            out.close();
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT id, title FROM movies WHERE MATCH(title) AGAINST(? IN BOOLEAN MODE) LIMIT 10";

            PreparedStatement preparedStatement = conn.prepareStatement(query);
            preparedStatement.setString(1, searchQuery + "*");

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", rs.getString("id"));
                jsonObject.addProperty("title", rs.getString("title"));
                jsonArray.add(jsonObject);
            }

            rs.close();

            // Write JSON string to output
            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            // Write error message JSON object to output
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}