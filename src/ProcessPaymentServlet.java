import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@WebServlet(name = "ProcessPaymentServlet", urlPatterns = "/api/process-payment")
public class ProcessPaymentServlet extends HttpServlet {
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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();

        HttpSession session = request.getSession();
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");

        String cardNumber = request.getParameter("cardNumber");
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String expiryDate = request.getParameter("expiryDate");

        if (cardNumber == null || firstName == null || lastName == null || expiryDate == null) {
            responseJsonObject.addProperty("success", false);
            responseJsonObject.addProperty("message", "Missing payment details.");
            out.write(responseJsonObject.toString());
            out.close();
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT c.id AS customer_id FROM customers c " +
                    "JOIN creditcards cc ON c.ccid = cc.id " +
                    "WHERE cc.id = ? AND cc.firstName = ? AND cc.lastName = ? AND cc.expiration = ?";

            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, cardNumber);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.setString(4, expiryDate);
            ResultSet rs;

            try {
                rs = statement.executeQuery();
            } catch (Exception e) {
                responseJsonObject.addProperty("success", false);
                responseJsonObject.addProperty("message", "Invalid card number");
                out.write(responseJsonObject.toString());
                out.close();
                return;
            }

            int customerId;
            if (rs.next()) {
                customerId = rs.getInt("customer_id");
            } else {
                responseJsonObject.addProperty("success", false);
                responseJsonObject.addProperty("message", "Credit card information not found.");
                response.getWriter().write(responseJsonObject.toString());
                return;
            }

            session.removeAttribute("saleIds");
            List<Integer> saleIds = new ArrayList<>();
            String insertSaleQuery = "INSERT INTO sales (customerId, movieId, saleDate, quantity) VALUES (?, ?, ?, ?)";
            PreparedStatement insertStatement = conn.prepareStatement(insertSaleQuery, PreparedStatement.RETURN_GENERATED_KEYS);
            LocalDate currentDate = LocalDate.now();

            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                String movieID = entry.getKey();
                int quantity = entry.getValue();

                insertStatement.setInt(1, customerId);
                insertStatement.setString(2, movieID);
                insertStatement.setDate(3, Date.valueOf(currentDate));
                insertStatement.setInt(4, quantity);
                insertStatement.executeUpdate();

                ResultSet generatedKeys = insertStatement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    saleIds.add(generatedKeys.getInt(1));
                }
            }

            session.setAttribute("saleIds", saleIds);
            cart.clear();

            responseJsonObject.addProperty("success", true);
            responseJsonObject.addProperty("customer_id", customerId);
            responseJsonObject.addProperty("redirect", request.getContextPath() + "/confirmation.html");
        } catch (Exception e) {
            responseJsonObject.addProperty("success", false);
            responseJsonObject.addProperty("message", "An error occurred: " + e.getMessage());
            response.setStatus(500);
        } finally {
            response.getWriter().write(responseJsonObject.toString());
            out.close();
        }

        out.write(responseJsonObject.toString());
        out.flush();
        out.close();
    }
}