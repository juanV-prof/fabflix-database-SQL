
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
import java.util.Random;

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

                String movieCheckQuery = "SELECT * FROM movies WHERE title = ? AND year = ? AND director = ?";
                PreparedStatement movieCheckStmt = conn.prepareStatement(movieCheckQuery);
                movieCheckStmt.setString(1, title);
                movieCheckStmt.setString(2, year);
                movieCheckStmt.setString(3, director);

                ResultSet movieRs = movieCheckStmt.executeQuery();
                if (movieRs.next()) {
                    jsonResponse.addProperty("errorMessage", "Movie already exists.");
                    response.setStatus(400);
                    movieCheckStmt.close();
                    movieRs.close();
                    out.write(jsonResponse.toString());
                    return;
                }

                String maxMovieIdQuery = "SELECT MAX(id) FROM movies";
                Statement maxIdStmt = conn.createStatement();
                ResultSet rs = maxIdStmt.executeQuery(maxMovieIdQuery);

                String newMovieId = "";

                if (rs.next()) {
                    String maxId = rs.getString(1);
                    if (maxId != null && maxId.matches("tt\\d+")) {
                        int numPart = Integer.parseInt(maxId.substring(2));
                        newMovieId = String.format("tt%07d", numPart + 1);
                    }
                }
                rs.close();
                maxIdStmt.close();

                String movieInsertQuery = "INSERT INTO movies (id, title, year, director, price) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement movieInsertStmt = conn.prepareStatement(movieInsertQuery);
                movieInsertStmt.setString(1, newMovieId);
                movieInsertStmt.setString(2, title);
                movieInsertStmt.setInt(3, Integer.parseInt(year));
                movieInsertStmt.setString(4, director);
                Random rand = new Random();
                movieInsertStmt.setInt(5, rand.nextInt(91) + 10);
                movieInsertStmt.executeUpdate();
                movieInsertStmt.close();

                float rating = (rand.nextInt(101) / 10.0f);
                int numVotes = rand.nextInt(300000) + 1;

                String ratingInsertQuery = "INSERT INTO ratings (movieId, rating, numVotes) VALUES (?, ?, ?)";
                PreparedStatement ratingInsertStmt = conn.prepareStatement(ratingInsertQuery);
                ratingInsertStmt.setString(1, newMovieId);
                ratingInsertStmt.setFloat(2, rating);
                ratingInsertStmt.setInt(3, numVotes);
                ratingInsertStmt.executeUpdate();
                ratingInsertStmt.close();

                String starCheckQuery = "SELECT id FROM stars WHERE name = ? AND birthYear = ?";
                PreparedStatement starCheckStmt = conn.prepareStatement(starCheckQuery);
                starCheckStmt.setString(1, starName);

                if (birthYear != null && !birthYear.isEmpty()) {
                    starCheckStmt.setInt(2, Integer.parseInt(birthYear));
                } else {
                    starCheckStmt.setNull(2, java.sql.Types.INTEGER);
                }

                ResultSet starResult = starCheckStmt.executeQuery();


                String starId = null;

                if (starResult.next()) {
                    starId = starResult.getString("id");
                } else {
                    String maxStarIdQuery = "SELECT MAX(id) FROM stars";
                    maxIdStmt = conn.createStatement();
                    rs = maxIdStmt.executeQuery(maxStarIdQuery);

                    String newStarId = "";

                    if (rs.next()) {
                        String maxId = rs.getString(1);
                        if (maxId != null && maxId.matches("nm\\d+")) {
                            int numPart = Integer.parseInt(maxId.substring(2));
                            newStarId = String.format("nm%07d", numPart + 1);
                        }
                    }
                    rs.close();
                    maxIdStmt.close();

                    String insertStarQuery = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
                    PreparedStatement starInsertStmt = conn.prepareStatement(insertStarQuery);
                    starInsertStmt.setString(1, newStarId);
                    starInsertStmt.setString(2, starName);


                    if (birthYear != null && !birthYear.isEmpty()) {
                        starInsertStmt.setInt(3, Integer.parseInt(birthYear));
                    } else {
                        starInsertStmt.setNull(3, java.sql.Types.INTEGER);
                    }
                    starInsertStmt.executeUpdate();
                    starId = newStarId;
                }

                String genreCheckQuery = "SELECT id FROM genres WHERE name = ?";
                PreparedStatement genreCheckStmt = conn.prepareStatement(genreCheckQuery);
                genreCheckStmt.setString(1, genreName);

                ResultSet genreResult = genreCheckStmt.executeQuery();

                int genreId = -1;

                if (genreResult.next()) {
                    genreId = genreResult.getInt("id");
                } else {
                    String maxGenreIdQuery = "SELECT MAX(id) FROM genres";
                    maxIdStmt = conn.createStatement();
                    rs = maxIdStmt.executeQuery(maxGenreIdQuery);

                    int newGenreId = 1;

                    if (rs.next()) {
                        String maxId = rs.getString(1);
                        if (maxId != null && maxId.matches("\\d+")) {
                            newGenreId = Integer.parseInt(maxId) + 1;
                        }
                    }
                    rs.close();
                    maxIdStmt.close();

                    String insertGenreQuery = "INSERT INTO genres (id, name) VALUES (?, ?)";
                    PreparedStatement genreInsertStmt = conn.prepareStatement(insertGenreQuery);
                    genreInsertStmt.setInt(1, newGenreId);
                    genreInsertStmt.setString(2, genreName);

                    genreInsertStmt.executeUpdate();
                    genreId = newGenreId;
                }

                String linkStarToMovieQuery = "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)";
                PreparedStatement linkStarStmt = conn.prepareStatement(linkStarToMovieQuery);
                linkStarStmt.setString(1, starId);
                linkStarStmt.setString(2, newMovieId);
                linkStarStmt.executeUpdate();
                linkStarStmt.close();


                String linkGenreToMovieQuery = "INSERT INTO genres_in_movies (genreId, movieId) VALUES (?, ?)";
                PreparedStatement linkGenreStmt = conn.prepareStatement(linkGenreToMovieQuery);
                linkGenreStmt.setInt(1, genreId);
                linkGenreStmt.setString(2, newMovieId);
                linkGenreStmt.executeUpdate();
                linkGenreStmt.close();


                jsonResponse.addProperty("movieId", newMovieId);
                jsonResponse.addProperty("starId", starId);
                jsonResponse.addProperty("genreId", genreId);


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