
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@WebServlet(name = "AddStarServlet", urlPatterns = "/api/add_star")
public class AddStarServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    // Initialize the dataSource from JNDI
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String starName = request.getParameter("starName");
        String birthYear = request.getParameter("birthYear");

        JsonObject jsonResponse = new JsonObject();

        if (starName == null || starName.isEmpty()) {
            jsonResponse.addProperty("errorMessage", "Missing required data.");
            response.setStatus(400);
        } else {
            try (Connection conn = dataSource.getConnection()) {
                String maxIdQuery = "SELECT MAX(id) FROM stars";
                Statement maxIdStmt = conn.createStatement();
                ResultSet rs = maxIdStmt.executeQuery(maxIdQuery);

                String newId = "";

                if (rs.next()) {
                    String maxId = rs.getString(1);
                    if (maxId != null && maxId.matches("nm\\d+")) {
                        int numPart = Integer.parseInt(maxId.substring(2));
                        newId = String.format("nm%07d", numPart + 1);
                    }
                }
                rs.close();
                maxIdStmt.close();

                String insertQuery = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(insertQuery);
                stmt.setString(1, newId);
                stmt.setString(2, starName);
                System.out.println(birthYear.length());
                if (birthYear != null && !birthYear.equals("")) {
                    System.out.println("GOES IN HERE");
                    stmt.setInt(3, Integer.parseInt(birthYear));
                } else {
                    stmt.setNull(3, java.sql.Types.INTEGER);
                }

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    jsonResponse.addProperty("message", "Star added successfully!");
                    response.setStatus(200);
                } else {
                    jsonResponse.addProperty("errorMessage", "Failed to add the star.");
                    response.setStatus(500);
                }

                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
                jsonResponse.addProperty("errorMessage", "Error: " + e.getMessage());
                response.setStatus(500);
            }
        }

        out.write(jsonResponse.toString());
        out.close();
    }
}