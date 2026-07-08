package com.accountingai.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/**
 * Produces preview artifacts for a PDF: a rendered image of the first page and
 * the document's extracted text. Used by the main UI's preview panel.
 *
 * <p>Rendering goes PDFBox {@link BufferedImage} -&gt; JavaFX {@link Image} via
 * {@link SwingFXUtils#toFXImage}, which is why the project depends on
 * {@code javafx-swing}.</p>
 */
public class PreviewService {

    /** DPI used when rasterizing the first page; 100 is a good screen-preview balance. */
    private static final float PREVIEW_DPI = 100f;

    /** Page index of the first page (PDFBox pages are zero-based). */
    private static final int FIRST_PAGE = 0;

    /**
     * Renders the first page of a PDF into a JavaFX image.
     *
     * @param pdf the PDF file
     * @return a JavaFX {@link Image} of page 1
     * @throws IOException if the file cannot be opened or rendered
     */
    public Image renderFirstPage(File pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            // Rasterize the first page at the preview DPI.
            BufferedImage bufferedImage = renderer.renderImageWithDPI(FIRST_PAGE, PREVIEW_DPI);
            // Convert the AWT image into a JavaFX image for display in an ImageView.
            return SwingFXUtils.toFXImage(bufferedImage, null);
        }
    }

    /**
     * Extracts the full text of a PDF (same idea as {@link PdfTextExtractor} but
     * accepting a {@link File}, which is what the preview panel already holds).
     *
     * @param pdf the PDF file
     * @return the extracted text
     * @throws IOException if the file cannot be opened or parsed
     */
    public String extractText(File pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }
}
