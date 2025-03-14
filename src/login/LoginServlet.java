package login;

import common.JwtUtil;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import org.jasypt.util.password.StrongPasswordEncryptor;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("Login request received in login service");
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");

        /* This example only allows username/password to be test/test
        /  in the real project, you should talk to the database to verify username/password
        */
        JsonObject responseJsonObject = new JsonObject();

        try {
            RecaptchaVerifyUtils.verify(gRecaptchaResponse);
        } catch (Exception e) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "reCAPTCHA verification failed: " + e.getMessage());
            response.getWriter().write(responseJsonObject.toString());
            return;
        }

        try {
            boolean validCreds = verifyCredentials(username, password);

            if (validCreds) {
                // Login success:

                // use username as the subject of JWT
                String subject = username;

                Map<String, Object> claims = new HashMap<>();

                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                claims.put("loginTime", dateFormat.format(new Date()));
                claims.put("role", "customer");

                HashMap<String, Integer> cart = new HashMap<>();
                claims.put("cart", cart);

                // Generate new JWT and add it to Header
                String token = JwtUtil.generateToken(subject, claims);
                JwtUtil.updateJwtCookie(request, response, token);

                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("message", "success");
            } else {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Incorrect email or password.");
            }
        } catch (Exception e) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "An error occurred: " + e.getMessage());
        }
        response.getWriter().write(responseJsonObject.toString());
    }

    private boolean verifyCredentials(String username, String password) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT password FROM customers WHERE email = ?")) {

            statement.setString(1, username);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String encryptedPassword = rs.getString("password");
                return new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;

    }
}
