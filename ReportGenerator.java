/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package poultry_farm;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;

public class ReportGenerator {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/poultry_farm";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "henry";

    // Custom page event helper for header
    static class HeaderFooter extends PdfPageEventHelper {
        private final String farmTitle;
        private final String reportTitle;
        private final String timestamp;
        private final Image logo;

        public HeaderFooter(String farmTitle, String reportTitle, String timestamp, Image logo) {
            this.farmTitle = farmTitle;
            this.reportTitle = reportTitle;
            this.timestamp = timestamp;
            this.logo = logo;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfPTable headerTable = new PdfPTable(2);
                headerTable.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                headerTable.setWidths(new float[]{1f, 3f}); // Left column (logo, timestamp): 25%, Right column (titles): 75%
                headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

                // Left column: Logo and Timestamp
                PdfPCell logoCell = new PdfPCell(logo, true);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setFixedHeight(30f); // Reduced height for compact header
                logoCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_TOP);
                headerTable.addCell(logoCell);

                // Right column: Farm Title and Report Title
                PdfPCell titleCell = new PdfPCell();
                titleCell.setBorder(Rectangle.NO_BORDER);
                titleCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                titleCell.addElement(new com.itextpdf.text.Phrase(farmTitle, 
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD)));
                titleCell.addElement(new com.itextpdf.text.Phrase(reportTitle, 
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10)));
                headerTable.addCell(titleCell);

                // Second row: Timestamp in left column, empty right column
                PdfPCell timestampCell = new PdfPCell(new com.itextpdf.text.Phrase(timestamp, 
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8)));
                timestampCell.setBorder(Rectangle.NO_BORDER);
                timestampCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_TOP);
                headerTable.addCell(timestampCell);
                headerTable.addCell(new PdfPCell()); // Empty cell

                // Position header at top, adjusted to avoid overlap
                headerTable.writeSelectedRows(0, -1, document.leftMargin(), document.getPageSize().getTop() - 80, writer.getDirectContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void generateReport(String reportName, String sqlQuery) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            pstmt = conn.prepareStatement(sqlQuery);
            rs = pstmt.executeQuery();

            // Get metadata for column names
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            String[] columnNames = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnNames[i] = metaData.getColumnLabel(i + 1);
            }

            // Create data structure for PDF
            java.util.List<Object[]> data = new java.util.ArrayList<>();
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                data.add(row);
            }

            // Generate PDF
            generatePDF(reportName, columnNames, data);

        } catch (SQLException | ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "Error generating report: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error generating PDF: " + ex.getMessage(), "PDF Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

   private static void generatePDF(String reportName, String[] columnNames, java.util.List<Object[]> data) throws Exception {
    // Get the desktop path of the current user
    String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";
    File desktopDir = new File(desktopPath);
    
    // Create reports directory on desktop if it doesn't exist
    File reportsDir = new File(desktopDir, "reports");
    if (!reportsDir.exists()) {
        reportsDir.mkdir();
    }

    // Create unique filename with timestamp
    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String fileName = reportsDir.getAbsolutePath() + File.separator + reportName + "_" + timestamp + ".pdf";

    // Initialize PDF document with increased top margin for header
    Document document = new Document();
    document.setMargins(36, 36, 100, 36); // Left, right, top, bottom margins (top increased to 100)
    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));

    // Add header with title, report name, timestamp, and logo
        Image logo;
    try {
        logo = Image.getInstance("resources/logo.png");
        logo.scaleToFit(50, 50);
    } catch (Exception e) {
        // Use a default image or continue without logo
        logo = null;
    }
    writer.setPageEvent(new HeaderFooter("Mbazira Poultry Farm", 
                                        "Poultry Farm Management System - " + reportName, 
                                        "Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), 
                                        logo));

    document.open();

    // Add spacing before table to ensure separation from header
    document.add(new Paragraph(" "));
    document.add(new Paragraph(" "));

    // Create table
    PdfPTable table = new PdfPTable(columnNames.length);
    table.setWidthPercentage(100);

    // Calculate column widths based on content length
    float[] columnWidths = new float[columnNames.length];
    float maxWidth = 300f; // Maximum width for any column (in points)
    float minWidth = 50f;  // Minimum width for any column (in points)
    for (int i = 0; i < columnNames.length; i++) {
        float maxLength = columnNames[i] != null ? columnNames[i].length() : 0;
        for (Object[] row : data) {
            String value = row[i] != null ? row[i].toString() : "";
            maxLength = Math.max(maxLength, value.length());
        }
        float width = Math.min(maxWidth, Math.max(minWidth, maxLength * 2f));
        columnWidths[i] = width;
    }
    table.setWidths(columnWidths);

    // Add column headers
    for (String columnName : columnNames) {
        PdfPCell cell = new PdfPCell(new com.itextpdf.text.Phrase(columnName));
        cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    // Add data rows
    for (Object[] row : data) {
        for (Object value : row) {
            String cellValue = value != null ? value.toString() : "";
            PdfPCell cell = new PdfPCell(new com.itextpdf.text.Phrase(cellValue));
            cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    // Add table to document
    document.add(table);

    // Close document
    document.close();
    
    // Show success message with the full path
    String message = "PDF report generated successfully!\n" +
                     "Saved to: " + fileName + "\n" +
                     "You can find it on your Desktop in the 'reports' folder.";
    JOptionPane.showMessageDialog(null, message, "Success", JOptionPane.INFORMATION_MESSAGE);
}
}