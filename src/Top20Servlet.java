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
import java.sql.ResultSet;
import java.sql.Statement;


// Declaring a WebServlet called Top20Servlet, which maps to url "/api/top20"
@WebServlet(name = "Top20Servlet", urlPatterns = "/api/top20")
public class Top20Servlet extends HttpServlet {
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

        response.setContentType("application/json"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {

            // Declare our statement
            Statement statement = conn.createStatement();

            String query = "WITH TopMovies AS (\n" +
                    "    SELECT \n" +
                    "        m.id, m.title, m.year, m.director, r.rating\n" +
                    "    FROM \n" +
                    "        movies m\n" +
                    "    JOIN \n" +
                    "        ratings r ON m.id = r.movieId\n" +
                    "    ORDER BY \n" +
                    "        r.rating DESC\n" +
                    "    LIMIT 20\n" +
                    ")\n" +
                    "SELECT \n" +
                    "    tm.id,\n" +
                    "    tm.title,\n" +
                    "    tm.year,\n" +
                    "    tm.director,\n" +
                    "    tm.rating,\n" +
                    "    REPLACE(GROUP_CONCAT(DISTINCT g.name SEPARATOR ', '), ',', ', ') AS genres,\n" +
                    "    SUBSTRING_INDEX(\n" +
                    "        GROUP_CONCAT(DISTINCT CONCAT(s.name, ':', s.id) SEPARATOR ', '),\n" +
                    "        ', ', 3\n" +
                    "    ) AS stars\n" +
                    "FROM \n" +
                    "    TopMovies tm\n" +
                    "LEFT JOIN \n" +
                    "    genres_in_movies gm ON tm.id = gm.movieId\n" +
                    "LEFT JOIN \n" +
                    "    genres g ON gm.genreId = g.id\n" +
                    "LEFT JOIN \n" +
                    "    stars_in_movies sm ON tm.id = sm.movieId\n" +
                    "LEFT JOIN \n" +
                    "    stars s ON sm.starId = s.id\n" +
                    "GROUP BY \n" +
                    "    tm.id, tm.title, tm.year, tm.director, tm.rating\n" +
                    "ORDER BY \n" +
                    "    tm.rating DESC;\n";

            // Perform the query
            ResultSet rs = statement.executeQuery(query);

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (rs.next()) {
                String movie_id = rs.getString("id");
                String title = rs.getString("title");
                String year = rs.getString("year");
                String director = rs.getString("director");
                String genres = rs.getString("genres"); // Comma-separated genres
                String stars = rs.getString("stars"); // Comma-separated stars
                Double rating = rs.getDouble("rating");

                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", movie_id);
                jsonObject.addProperty("title", title);
                jsonObject.addProperty("year", year);
                jsonObject.addProperty("director", director);
                jsonObject.addProperty("genres", genres);
                jsonObject.addProperty("stars", stars);
                jsonObject.addProperty("rating", rating);

                jsonArray.add(jsonObject);
            }
            rs.close();
            statement.close();

            // Log to localhost log
            request.getServletContext().log("Retrieved " + jsonArray.size() + " top-rated movies");

            // Write JSON string to output
            out.write(jsonArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {

            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }
}
