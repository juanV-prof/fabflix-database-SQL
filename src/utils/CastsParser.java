package utils;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;

public class CastsParser extends DefaultHandler {
    private Connection conn;
    private PrintWriter reportWriter;
    private PrintWriter outputWriter;
    private String tempValue;
    private String actorName;
    private String movieTitle;
    private boolean testing = false;

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
                        if (testing) {
                            outputWriter.println("Testing - Star: " + actorName + " -> Movie: " + movieTitle);
                        } else {
                            insertEntry(starId, movieId);
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
        String query = "SELECT id FROM movies WHERE title = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, title);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id");
                }
            }
        }
        return null;
    }

    private String getStarIdByName(String name) throws SQLException {
        String query = "SELECT id FROM stars WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id");
                }
            }
        }
        return null;
    }

    private boolean insertEntry(String starId, String movieId) throws SQLException {
        String insertQuery = "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
            insertStmt.setString(1, starId);
            insertStmt.setString(2, movieId);
            insertStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void closeConnection() {
        try {
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