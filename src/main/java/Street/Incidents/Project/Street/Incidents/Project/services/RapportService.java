package Street.Incidents.Project.Street.Incidents.Project.services;

import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Rapport;
import Street.Incidents.Project.Street.Incidents.Project.entities.User;
import Street.Incidents.Project.Street.Incidents.Project.repositories.RapportRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class RapportService {

    private final RapportRepository rapportRepository;

    /**
     * Generate PDF report for an incident
     */
    public byte[] genererRapportPDF(Incident incident, User user) throws DocumentException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, baos);
        document.open();

        // Fonts
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLACK);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);

        // Title
        Paragraph title = new Paragraph("RAPPORT D'INCIDENT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Separator line
        document.add(new Paragraph("_________________________________________________________________"));
        document.add(Chunk.NEWLINE);

        // General information
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(10);

        addTableRow(infoTable, "ID Incident:", "#" + incident.getId(), headerFont, normalFont);
        addTableRow(infoTable, "Titre:", incident.getTitre(), headerFont, normalFont);
        addTableRow(infoTable, "Categorie:", incident.getCategorie().name(), headerFont, normalFont);
        addTableRow(infoTable, "Priorite:", incident.getPriorite().getLabel(), headerFont, normalFont);
        addTableRow(infoTable, "Statut:", formatStatut(incident.getStatut().name()), headerFont, normalFont);



        if (incident.getLatitude() != null && incident.getLongitude() != null) {
            addTableRow(infoTable, "Coordonnees:",
                    "Lat: " + incident.getLatitude() + ", Long: " + incident.getLongitude(),
                    headerFont, normalFont);
        }

        String dateCreation = incident.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        addTableRow(infoTable, "Date de creation:", dateCreation, headerFont, normalFont);

        if (incident.getDateResolution() != null) {
            String dateResolution = incident.getDateResolution().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            addTableRow(infoTable, "Date de resolution:", dateResolution, headerFont, normalFont);
        }

        document.add(infoTable);

        // Description
        if (incident.getDescription() != null && !incident.getDescription().isEmpty()) {
            document.add(Chunk.NEWLINE);
            Paragraph descTitle = new Paragraph("Description:", headerFont);
            document.add(descTitle);

            Paragraph description = new Paragraph(incident.getDescription(), normalFont);
            description.setAlignment(Element.ALIGN_JUSTIFIED);
            description.setSpacingBefore(5);
            description.setSpacingAfter(10);
            document.add(description);
        }

        // Commentaire citoyen
        if (incident.getCommentaireCitoyen() != null && !incident.getCommentaireCitoyen().isEmpty()) {
            document.add(Chunk.NEWLINE);
            Paragraph commentTitle = new Paragraph("Commentaire citoyen:", headerFont);
            document.add(commentTitle);

            Paragraph comment = new Paragraph(incident.getCommentaireCitoyen(), normalFont);
            comment.setAlignment(Element.ALIGN_JUSTIFIED);
            comment.setSpacingBefore(5);
            comment.setSpacingAfter(10);
            document.add(comment);
        }

        // Reported by
        if (incident.getDeclarant() != null) {
            document.add(Chunk.NEWLINE);
            Paragraph reportedBy = new Paragraph(
                    "Signale par: " + incident.getDeclarant().getNom() + " " +
                            incident.getDeclarant().getPrenom(),
                    normalFont
            );
            document.add(reportedBy);
        }

        // Agent assigned
        if (incident.getAgent() != null) {
            Paragraph agentInfo = new Paragraph(
                    "Agent assigne: " + incident.getAgent().getNom() + " " +
                            incident.getAgent().getPrenom(),
                    normalFont
            );
            document.add(agentInfo);
        }

        // Footer
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("_________________________________________________________________"));
        Paragraph footer = new Paragraph(
                "Rapport genere le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();

        // Save report to database
        sauvegarderRapport(incident, user, "PDF", "Rapport PDF de l'incident #" + incident.getId());

        return baos.toByteArray();
    }

    /**
     * Generate CSV report for an incident
     */
    public String genererRapportCSV(Incident incident, User user) {
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Champ,Valeur\n");

        // Data
        csv.append("ID,").append(incident.getId()).append("\n");
        csv.append("Titre,\"").append(escapeCSV(incident.getTitre())).append("\"\n");
        csv.append("Categorie,").append(incident.getCategorie().name()).append("\n");
        csv.append("Priorite,").append(incident.getPriorite().getLabel()).append("\n");
        csv.append("Statut,").append(formatStatut(incident.getStatut().name())).append("\n");

        String localisation = incident.getQuartier() != null ?
                incident.getQuartier().getMunicipalite() : "Non definie";
        csv.append("Localisation,").append(localisation).append("\n");

        if (incident.getLatitude() != null && incident.getLongitude() != null) {
            csv.append("Latitude,").append(incident.getLatitude()).append("\n");
            csv.append("Longitude,").append(incident.getLongitude()).append("\n");
        }

        if (incident.getDescription() != null) {
            csv.append("Description,\"").append(escapeCSV(incident.getDescription())).append("\"\n");
        }

        if (incident.getCommentaireCitoyen() != null) {
            csv.append("Commentaire citoyen,\"").append(escapeCSV(incident.getCommentaireCitoyen())).append("\"\n");
        }

        String dateCreation = incident.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        csv.append("Date de creation,").append(dateCreation).append("\n");

        if (incident.getDateResolution() != null) {
            String dateResolution = incident.getDateResolution().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            csv.append("Date de resolution,").append(dateResolution).append("\n");
        }

        if (incident.getDeclarant() != null) {
            csv.append("Signale par,\"").append(
                    escapeCSV(incident.getDeclarant().getNom() + " " + incident.getDeclarant().getPrenom())
            ).append("\"\n");
        }

        if (incident.getAgent() != null) {
            csv.append("Agent assigne,\"").append(
                    escapeCSV(incident.getAgent().getNom() + " " + incident.getAgent().getPrenom())
            ).append("\"\n");
        }

        // Save report to database
        sauvegarderRapport(incident, user, "CSV", "Rapport CSV de l'incident #" + incident.getId());

        return csv.toString();
    }

    /**
     * Add a row to the PDF table
     */
    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(new BaseColor(240, 240, 240));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    /**
     * Format status for display
     */
    private String formatStatut(String statut) {
        return switch (statut) {
            case "SIGNALE" -> "Signale";
            case "PRIS_EN_CHARGE" -> "Pris en Charge";
            case "EN_RESOLUTION" -> "En Resolution";
            case "RESOLU" -> "Resolu";
            case "CLOTURE" -> "Cloture";
            default -> statut;
        };
    }

    /**
     * Escape special characters for CSV
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    /**
     * Save report to database
     */
    private void sauvegarderRapport(Incident incident, User user, String type, String contenu) {
        Rapport rapport = Rapport.builder()
                .dateGeneration(LocalDate.now())
                .type(type)
                .contenu(contenu)
                .generePar(user)
                .build();

        rapportRepository.save(rapport);
    }
}