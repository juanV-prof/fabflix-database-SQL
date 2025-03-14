import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;


// Declaring a WebServlet called CheckoutServlet, which maps to url "/api/checkout"
@WebServlet(name = "CheckoutServlet", urlPatterns = "/api/checkout")
public class CheckoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Create a dataSource which registered in web.
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession();
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");


        if (cart == null || cart.isEmpty()) {
            out.write("[]");
            response.setStatus(200);
            return;
        }

        JsonArray jsonArray = new JsonArray();

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT id, title, price FROM movies WHERE id = ?";
            PreparedStatement statement = conn.prepareStatement(query);

            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                String movieId = entry.getKey();
                int quantity = entry.getValue();

                statement.setString(1, movieId);
                ResultSet rs = statement.executeQuery();

                if (rs.next()) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("movie_id", movieId);
                    jsonObject.addProperty("title", rs.getString("title"));
                    jsonObject.addProperty("price", rs.getInt("price"));
                    jsonObject.addProperty("quantity", quantity);
                    jsonArray.add(jsonObject);
                }
                rs.close();
            }
            statement.close();

            request.getServletContext().log("Retrieved cart items: " + jsonArray.size());
            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", e.getMessage());
            out.write(errorJson.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
