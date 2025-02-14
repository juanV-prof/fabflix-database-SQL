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
import java.util.HashSet;
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
    private HashSet<String> existingMaps = new HashSet<>();
    private List<String[]> batchInsert = new ArrayList<>();
    private static final int BATCH_SIZE = 5000;

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

            preloadData();
            preloadExistingMaps();

        } catch (Exception e) {
            System.out.println("Database connection issue!");
            e.printStackTrace();
        }
    }

    public void parseDocument(String xmlFile) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
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

                String movieId = movieCache.get(movieTitle);
                String starId = starCache.get(actorName);

                if (movieId != null && starId != null) {
                    String map = starId + "-" + movieId;
                    if (!existingMaps.contains(map)) {
                        batchInsert.add(new String[]{starId, movieId}); // Instead of adding each time, add to batch
                        existingMaps.add(map);

                        if (batchInsert.size() >= BATCH_SIZE) { // When batch reaches BATCH_SIZE, execute them
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

    private void preloadData() throws SQLException {
        String movieQuery = "SELECT id, title FROM movies";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(movieQuery)) {
            while (rs.next()) {
                movieCache.put(rs.getString("title"), rs.getString("id"));
            }
        }

        String starQuery = "SELECT id, name FROM stars";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(starQuery)) {
            while (rs.next()) {
                starCache.put(rs.getString("name"), rs.getString("id"));
            }
        }
    }

    private void preloadExistingMaps() throws SQLException {
        System.out.println("Preloading existing stars_in_movies data...");
        String query = "SELECT starId, movieId FROM stars_in_movies";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                existingMaps.add(rs.getString("starId") + "|" + rs.getString("movieId"));
            }
        }
    }

    private void insertEntriesBatch() throws SQLException {
        if (batchInsert.isEmpty()) return;

        String insertQuery = "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
            for (String[] entry : batchInsert) {
                insertStmt.setString(1, entry[0]);
                insertStmt.setString(2, entry[1]);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
            batchInsert.clear();
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