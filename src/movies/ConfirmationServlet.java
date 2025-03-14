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
import java.util.List;


// Declaring a WebServlet called ConfirmationServlet, which maps to url "/api/confirmation"
@WebServlet(name = "ConfirmationServlet", urlPatterns = "/api/confirmation")
public class ConfirmationServlet extends HttpServlet {
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
        List<Integer> saleIds = (List<Integer>) session.getAttribute("saleIds");

        JsonArray jsonArray = new JsonArray();

        try (Connection conn = dataSource.getConnection()) {
            String placeholders = String.join(",", java.util.Collections.nCopies(saleIds.size(), "?"));
            String query = "SELECT s.id AS sale_id, s.movieId, s.quantity, m.title, m.price " +
                    "FROM sales s JOIN movies m ON s.movieId = m.id " +
                    "WHERE s.id IN (" + placeholders + ")";

            PreparedStatement statement = conn.prepareStatement(query);

            for (int i = 0; i < saleIds.size(); i++) {
                statement.setInt(i + 1, saleIds.get(i));
            }

            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                JsonObject movieJson = new JsonObject();
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                double totalPrice = quantity * price;

                movieJson.addProperty("sale_id", rs.getInt("sale_id"));
                movieJson.addProperty("movie_id", rs.getString("movieId"));
                movieJson.addProperty("title", rs.getString("title"));
                movieJson.addProperty("quantity", quantity);
                movieJson.addProperty("price", price);
                movieJson.addProperty("total_price", totalPrice);

                jsonArray.add(movieJson);
            }

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
