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
import java.util.HashMap;

@WebServlet(name = "CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {
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
        HttpSession session = request.getSession();
        HashMap<String, Integer> cart = (HashMap<String, Integer>) session.getAttribute("cart");

        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        String movieId = request.getParameter("movieId");
        String action = request.getParameter("action");
        String quantityStr = request.getParameter("quantity");

        if ("add".equals(action)) {
            cart.put(movieId, cart.getOrDefault(movieId, 0) + 1);
        } else if ("remove".equals(action)) {
            cart.remove(movieId);
        } else if ("update".equals(action)) {
            int quantity = Integer.parseInt(quantityStr);
            cart.put(movieId, quantity);
        }

        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("status", "success");
        response.getWriter().write(responseJsonObject.toString());
    }
}
