package utils;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class MainsParser extends DefaultHandler {
    private Connection conn;
    private PrintWriter logWriter;
    private PrintWriter reportWriter;
    private String tempValue;
    private String directorName;
    private String movieTitle, movieYear;
    private HashSet<String> genres = new HashSet<>();
    private boolean testing = false;
    private final Random random = new Random();
    private int nextMovieNum;
    private int nextGenreId;

    private HashMap<String, Integer> genreIdMap = new HashMap<>();

    public MainsParser(boolean testing) {
        this.testing = testing;
        try {
            conn = new SQLConnector().getConnection();
            if (conn != null) {
                System.out.println("Connected to MySQL successfully using SQLConnector!");
                initializeNextIds();
            } else {
                System.out.println("Failed to establish database connection.");
            }

            logWriter = new PrintWriter(new FileWriter("output.txt", false));
            reportWriter = new PrintWriter(new FileWriter("report.txt", false));

        } catch (Exception e) {
            System.out.println("Database connection issue!");
            e.printStackTrace();
        }
    }

    private void initializeNextIds() throws SQLException {
        this.nextMovieNum = getNextMovieNumber();
        this.nextGenreId = getNextAvailableId("genres", "id");
        loadGenreIds();
    }

    private int getNextMovieNumber() throws SQLException {
        String query = "SELECT MAX(SUBSTRING(id, 3) + 0) FROM movies WHERE id LIKE 'tt%'";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getObject(1) != null) {
                return rs.getInt(1) + 1;
            }
        }
        return 0;
    }

    private int getNextAvailableId(String tableName, String column) throws SQLException {
        String query = "SELECT MAX(" + column + ") FROM " + tableName;
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getObject(1) != null) {
                return rs.getInt(1) + 1;
            }
        }
        return 1;
    }

    private void loadGenreIds() throws SQLException {
        String query = "SELECT id, name FROM genres";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                genreIdMap.put(rs.getString("name"), rs.getInt("id"));
            }
        }
    }

    public void parseDocument(String xmlFile) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(xmlFile, this);
        } catch (Exception e) {
            e.printStackTrace();
            reportWriter.println("Could not parse XML file - " + e.getMessage());
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        tempValue = "";
        if (qName.equalsIgnoreCase("film")) {
            movieTitle = movieYear = null;
            genres.clear();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        tempValue += new String(ch, start, length).trim();
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        try {
            if (qName.equalsIgnoreCase("dirname")) {
                directorName = tempValue.isEmpty() ? "unknown" : tempValue;
            } else if (qName.equalsIgnoreCase("t")) {
                movieTitle = tempValue.isEmpty() ? null : tempValue;
            } else if (qName.equalsIgnoreCase("year")) {
                movieYear = tempValue.isEmpty() ? null : tempValue;
            } else if (qName.equalsIgnoreCase("cat")) {
                if (!tempValue.isEmpty()) {
                    genres.add(tempValue);
                }
            } else if (qName.equalsIgnoreCase("film")) {
                if (movieTitle == null || movieYear == null) {
                    reportWriter.println("Film has missing data - Title: " +
                            (movieTitle != null ? movieTitle : "N/A") +
                            ", Year: " + (movieYear != null ? movieYear : "N/A") +
                            ", Director: " + (directorName != null ? directorName : "N/A"));
                    return;
                }

                if (directorName.toLowerCase().contains("unknown")) {
                    reportWriter.println("Unknown director - Title: " + movieTitle +
                            " (" + movieYear + "), Director: " + directorName);
                }

                String movieId = generateNextMovieId();
                int price = generateRandomPrice();
                double rating = generateRandomRating();
                int numVotes = generateRandomNumVotes();

                if (!testing) {
                    try {
                        boolean insertedMovie = insertMovie(movieId, movieTitle, movieYear, directorName, price);
                        if (insertedMovie) {
                            insertRating(movieId, rating, numVotes);
                            for (String genre : genres) {
                                int genreId = getGenreId(genre);
                                linkMovieGenre(movieId, genreId);
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    logWriter.println("Testing - Movie ID: " + movieId + " | Title: " + movieTitle + " (" + movieYear + ") | Director: " + directorName + " | Price: $" + price);
                    logWriter.println("Testing - Rating Entry -> Movie ID: " + movieId + " | Rating: " + rating + " (" + numVotes + " votes)");
                    for (String genre : genres) {
                        logWriter.println("Testing - Genre ID: " + getGenreId(genre) + " | Genre: " + genre);
                    }
                }
            }
        } catch (SQLException e) {
            reportWriter.println("SQL Exception encountered - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateNextMovieId() {
        return "tt" + String.format("%07d", nextMovieNum++);
    }

    private int getGenreId(String genre) throws SQLException {
        if (genreIdMap.containsKey(genre)) {
            return genreIdMap.get(genre);
        }

        String query = "SELECT id FROM genres WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, genre);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int existingGenreId = rs.getInt("id");
                    genreIdMap.put(genre, existingGenreId);
                    return existingGenreId;
                }
            }
        }

        int newGenreId = nextGenreId++;
        insertGenre(newGenreId, genre);
        genreIdMap.put(genre, newGenreId);
        return newGenreId;
    }

    private boolean insertMovie(String id, String title, String year, String directorName, int price) throws SQLException {
        String checkQuery = "SELECT id FROM movies WHERE TRIM(LOWER(title)) = TRIM(LOWER(?)) AND year = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
            checkStmt.setString(1, title);
            checkStmt.setString(2, year);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    reportWriter.println(title + " (" + year + ")" + " already exists");
                    return false;
                }
            }
        } catch (SQLException e) {
            return false;
        }

        String insertQuery = "INSERT INTO movies (id, title, year, director, price) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
            insertStmt.setString(1, id);
            insertStmt.setString(2, title.trim());
            insertStmt.setString(3, year);
            insertStmt.setString(4, directorName);
            insertStmt.setInt(5, price);
            insertStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private void insertGenre(int id, String name) throws SQLException {
        String query = "INSERT INTO genres (id, name) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.executeUpdate();
        }
    }

    private void linkMovieGenre(String movieId, int genreId) throws SQLException {
        String query = "INSERT INTO genres_in_movies (movieId, genreId) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, movieId);
            stmt.setInt(2, genreId);
            stmt.executeUpdate();
        }
    }

    private void insertRating(String movieId, double rating, int numVotes) throws SQLException {
        String query = "INSERT INTO ratings (movieId, rating, numVotes) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, movieId);
            stmt.setDouble(2, rating);
            stmt.setInt(3, numVotes);
            stmt.executeUpdate();
        }
    }

    public void closeConnection() {
        try {
            if (conn != null) conn.close();
            if (logWriter != null) logWriter.close();
            if (reportWriter != null) reportWriter.close();
            System.out.println("Output written to output.txt");
            System.out.println("Report written to report.txt");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int generateRandomPrice() {
        return random.nextInt(91) + 10;
    }

    private double generateRandomRating() {
        return Math.round((random.nextDouble() * 10.0) * 10.0) / 10.0;
    }

    private int generateRandomNumVotes() {
        return random.nextInt(300000) + 1;
    }


    public static void main(String[] args) {
        String xmlFile = args[0];
        boolean testing = false;

        MainsParser parser = new MainsParser(testing);
        parser.parseDocument(xmlFile);
        parser.closeConnection();
    }

}