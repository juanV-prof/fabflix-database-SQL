package utils;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CastsParser extends DefaultHandler {
    private Connection conn;
    private PrintWriter reportWriter;
    private PrintWriter outputWriter;
    private String tempValue;
    private String actorName;
    private String movieTitle;
    private boolean testing = false;
    private HashMap<String, String> movieCache = new HashMap<>();
    private HashMap<String, String> starCache = new HashMap<>();
    private List<String[]> batchEntries = new ArrayList<>();

    // Added hashmaps and array to optimize parsing and lower the time it takes

    public CastsParser(boolean testing) {
        this.testing = testing;
        try {
            conn = new SQLConnector().getConnection();
            if (conn != null) {
                System.out.println("Connected to MySQL successfully using SQLConnector!");
            } else {
                System.out.println("Failed to establish database connection.");
            }

            reportWriter = new PrintWriter(new FileWriter("report.txt", true));
            if (testing) {
                outputWriter = new PrintWriter(new FileWriter("output.txt", false));
            }

        } catch (Exception e) {
            System.out.println("Database connection issue!");
            e.printStackTrace();
        }
    }

    public void parseDocument(String xmlFile) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(xmlFile, this);
        } catch (Exception e) {
            e.printStackTrace();
            reportWriter.println("\nFailed to parse XML file - " + e.getMessage());
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        tempValue = "";
        if (qName.equalsIgnoreCase("m")) {
            actorName = null;
            movieTitle = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        tempValue += new String(ch, start, length).trim();
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        try {
            if (qName.equalsIgnoreCase("a")) {
                actorName = tempValue.isEmpty() ? null : tempValue;
            } else if (qName.equalsIgnoreCase("t")) {
                movieTitle = tempValue.isEmpty() ? null : tempValue;
            } else if (qName.equalsIgnoreCase("m")) {
                if (actorName == null || movieTitle == null) {
                    reportWriter.println("Skipping entry due to missing actor or movie title.");
                    return;
                }

                String movieId = getMovieIdByTitle(movieTitle);
                String starId = getStarIdByName(actorName);

                if (movieId != null && starId != null) {
                    if (!entryExists(starId, movieId)) {
                        batchEntries.add(new String[]{starId, movieId}); // Instead of adding each time, add to batch

                        if (batchEntries.size() >= 500) { // When batch reaches 500, execute them
                            insertEntriesBatch();
                        }
                    } else {
                        reportWriter.println("Duplicate entry found: " + actorName + " -> " + movieTitle);
                    }
                } else {
                    if (movieId == null) reportWriter.println("Movie not found: " + movieTitle);
                    if (starId == null) reportWriter.println("Actor not found: " + actorName);
                }
            }
        } catch (SQLException e) {
            reportWriter.println("SQL Exception - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean entryExists(String starId, String movieId) throws SQLException {
        String query = "SELECT 1 FROM stars_in_movies WHERE starId = ? AND movieId = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, starId);
            stmt.setString(2, movieId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String getMovieIdByTitle(String title) throws SQLException {
        if (movieCache.containsKey(title)) { // Keep the movieId cached to reduce calls to sql
            return movieCache.get(title);
        }

        String query = "SELECT id FROM movies WHERE title = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, title);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String movieId = rs.getString("id");
                    movieCache.put(title, movieId); // Add movieId to cache
                    return movieId;
                }
            }
        }
        return null;
    }

    private String getStarIdByName(String name) throws SQLException {
        if (starCache.containsKey(name)) { // Keep the starId cached to reduce calls to sql
            return starCache.get(name);
        }

        String query = "SELECT id FROM stars WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String starId = rs.getString("id");
                    starCache.put(name, starId); // Add starId to cache
                    return starId;
                }
            }
        }
        return null;
    }

    private void insertEntriesBatch() throws SQLException {
        if (batchEntries.isEmpty()) return;

        String insertQuery = "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
            for (String[] entry : batchEntries) {
                insertStmt.setString(1, entry[0]);
                insertStmt.setString(2, entry[1]);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
            batchEntries.clear();
        }
    }

    public void closeConnection() {
        try {
            insertEntriesBatch();
            if (conn != null) conn.close();
            if (reportWriter != null) reportWriter.close();
            if (testing && outputWriter != null) outputWriter.close();
            System.out.println("Report written to report.txt");
            if (testing) {
                System.out.println("Testing output written to output.txt");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String xmlFile = "/Users/aleon/UCI/CS_122B/XML/casts124.xml";
        boolean testing = false;

        CastsParser parser = new CastsParser(testing);
        parser.parseDocument(xmlFile);
        parser.closeConnection();
    }

}