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
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;


// Declaring a WebServlet called ResultsServlet, which maps to url "/api/top20"
@WebServlet(name = "ResultsServlet", urlPatterns = "/api/results")
public class ResultsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Create a dataSource which registered in web.
    private DataSource dataSource;

    private static final Map<String, String> orderByMap = new HashMap<>();
    static {
        orderByMap.put("titleAscRatingAsc", "ORDER BY title ASC, rating ASC");
        orderByMap.put("titleAscRatingDesc", "ORDER BY title ASC, rating DESC");
        orderByMap.put("titleDescRatingAsc", "ORDER BY title DESC, rating ASC");
        orderByMap.put("titleDescRatingDesc", "ORDER BY title DESC, rating DESC");
        orderByMap.put("ratingAscTitleAsc", "ORDER BY rating ASC, title ASC");
        orderByMap.put("ratingAscTitleDesc", "ORDER BY rating ASC, title DESC");
        orderByMap.put("ratingDescTitleAsc", "ORDER BY rating DESC, title ASC");
        orderByMap.put("ratingDescTitleDesc", "ORDER BY rating DESC, title DESC");
    }

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

            String genre = request.getParameter("genre");
            String prefix = request.getParameter("prefix");
            String title = request.getParameter("title");
            String year = request.getParameter("year");
            String star = request.getParameter("star");
            int moviesPerPage = Integer.parseInt(request.getParameter("moviesPerPage"));
            String sortBy = request.getParameter("sortBy");
            int pageNumber = Integer.parseInt(request.getParameter("pageNumber"));
            int offset = moviesPerPage * (pageNumber - 1);

            String query = "";
            PreparedStatement statement;
            String filter = orderByMap.get(sortBy);

            if (genre != null) {
                query = "WITH TopMovies AS (\n" +
                        "    SELECT \n" +
                        "        m.id, m.title, m.year, m.director, r.rating\n" +
                        "    FROM \n" +
                        "        movies m\n" +
                        "    JOIN \n" +
                        "        ratings r ON m.id = r.movieId\n" +
                        filter +
                        "    LIMIT ? OFFSET ?\n" +
                        ")\n" +
                        "-- Fetch genres and stars for these top movies\n" +
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
                        filter;

                statement = conn.prepareStatement(query);
                statement.setInt(1, moviesPerPage);
                statement.setInt(2, offset);
            } else {
                statement = conn.prepareStatement("");
            }

            // Perform the query
            ResultSet rs = statement.executeQuery(query);

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (rs.next()) {
                String movie_id = rs.getString("id");
                String resTitle = rs.getString("title");
                String resYear = rs.getString("year");
                String director = rs.getString("director");
                String genres = rs.getString("genres"); // Comma-separated genres
                String stars = rs.getString("stars"); // Comma-separated stars
                Double rating = rs.getDouble("rating");

                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", movie_id);
                jsonObject.addProperty("title", resTitle);
                jsonObject.addProperty("year", resYear);
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
