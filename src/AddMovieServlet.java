
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
import java.sql.*;

@WebServlet(name = "AddMovieServlet", urlPatterns = "/api/add_movie")
public class AddMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
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

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String starName = request.getParameter("starName");
        String birthYear = request.getParameter("birthYear");
        String genreName = request.getParameter("genreName");

        System.out.println("title: " + title);

        JsonObject jsonResponse = new JsonObject();

        if (title == null || title.isEmpty() || year == null || year.isEmpty() || director == null || director.isEmpty()) {
            jsonResponse.addProperty("errorMessage", "Missing required data.");
            response.setStatus(400);
        } else {
            try (Connection conn = dataSource.getConnection()) {
                String std_pcd = "CALL add_movie(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                CallableStatement stmt = conn.prepareCall(std_pcd);

                stmt.setString(1, title);
                stmt.setInt(2, Integer.parseInt(year));
                stmt.setString(3, director);
                stmt.setString(4, starName);

                if (birthYear != null && !birthYear.isEmpty()) {
                    stmt.setInt(5, Integer.parseInt(birthYear));
                } else {
                    stmt.setNull(5, java.sql.Types.INTEGER);
                }

                stmt.setString(6, genreName);

                stmt.registerOutParameter(7, Types.VARCHAR);
                stmt.registerOutParameter(8, Types.VARCHAR);
                stmt.registerOutParameter(9, Types.INTEGER);
                stmt.registerOutParameter(10, Types.BOOLEAN);

                stmt.execute();

                String movieID = stmt.getString(7);
                String starID = stmt.getString(8);
                int genreID = stmt.getInt(9);
                boolean success = stmt.getBoolean(10);

                if (success) {
                    jsonResponse.addProperty("movieId", movieID);
                    jsonResponse.addProperty("starId", starID);
                    jsonResponse.addProperty("genreId", genreID);
                    response.setStatus(200);
                } else {
                    jsonResponse.addProperty("errorMessage", "Failed to add the movie. It may already exist.");
                    response.setStatus(400);
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