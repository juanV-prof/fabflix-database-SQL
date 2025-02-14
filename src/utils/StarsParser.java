package utils;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;

public class StarsParser extends DefaultHandler {
    private Connection conn;
    private PrintWriter reportWriter;
    private PrintWriter outputWriter;
    private String tempValue;
    private String actorName;
    private String birthYear;
    private boolean testing = false;
    private int nextStarId;

    public StarsParser(boolean testing) {
        this.testing = testing;
        try {
            conn = new SQLConnector().getConnection();
            if (conn != null) {
                System.out.println("Connected to MySQL successfully using SQLConnector!");
                nextStarId = getNextStarId();
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
        if (qName.equalsIgnoreCase("actor")) {
            actorName = null;
            birthYear = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        tempValue += new String(ch, start, length).trim();
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        try {
            if (qName.equalsIgnoreCase("stagename")) {
                actorName = tempValue.isEmpty() ? null : tempValue;
            } else if (qName.equalsIgnoreCase("dob")) {
                birthYear = tempValue.isEmpty() ? null : tempValue;
            } else if (qName.equalsIgnoreCase("actor")) {
                if (actorName == null) {
                    return;
                }

                if (!actorExists(actorName)) {
                    String starId = generateNextStarId();

                    if (testing) {
                        outputWriter.println("Testing - Actor ID: " + starId + ", Name: " + actorName +
                                ", Birth Year: " + (birthYear != null ? birthYear : "NULL"));
                    } else {
                        insertActor(starId, actorName, birthYear);
                    }
                }
            }
        } catch (SQLException e) {
            reportWriter.println("SQL Exception - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean actorExists(String name) throws SQLException {
        String query = "SELECT id FROM stars WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int getNextStarId() throws SQLException {
        String query = "SELECT MAX(SUBSTRING(id, 3) + 0) FROM stars WHERE id LIKE 'nm%'";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getObject(1) != null) {
                return rs.getInt(1) + 1;
            }
        }
        return 1;
    }

    private String generateNextStarId() {
        return "nm" + String.format("%07d", nextStarId++);
    }

    private boolean insertActor(String id, String name, String birthYear) throws SQLException {
        Integer birthYearInt;
        try {
            birthYearInt = (birthYear != null) ? Integer.valueOf(birthYear) : null;
        } catch (NumberFormatException e) {
            birthYearInt = null;
        }

        String insertQuery = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
            insertStmt.setString(1, id);
            insertStmt.setString(2, name.trim());

            if (birthYearInt != null) {
                insertStmt.setInt(3, birthYearInt);
            } else {
                insertStmt.setNull(3, Types.INTEGER);
            }

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
        String xmlFile = args[0];
        boolean testing = false;

        StarsParser parser = new StarsParser(testing);
        parser.parseDocument(xmlFile);
        parser.closeConnection();
    }
}