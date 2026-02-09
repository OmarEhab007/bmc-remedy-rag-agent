package com.bmc.rag.vectorization.tika;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AttachmentParser using temp files.
 */
class AttachmentParserTest {

    @TempDir
    Path tempDir;

    private AttachmentParser parser;
    private Path testFile;

    @BeforeEach
    void setUp() {
        parser = new AttachmentParser();
    }

    @AfterEach
    void tearDown() {
        parser.shutdown();
    }

    @Test
    void parse_plainTextFile_extractsContent() throws IOException {
        // Given
        String content = "This is a plain text file.\nWith multiple lines.";
        testFile = createTempFile("test.txt", content);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isPresent();
        AttachmentParser.ParsedContent parsed = result.get();
        assertThat(parsed.text()).contains("This is a plain text file");
        assertThat(parsed.text()).contains("With multiple lines");
        assertThat(parsed.mimeType()).contains("text/plain");
        assertThat(parsed.filename()).isEqualTo("test.txt");
    }

    @Test
    void parse_htmlFile_extractsTextContent() throws IOException {
        // Given
        String htmlContent = "<html><body><h1>Title</h1><p>This is a paragraph.</p></body></html>";
        testFile = createTempFile("test.html", htmlContent);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isPresent();
        AttachmentParser.ParsedContent parsed = result.get();
        assertThat(parsed.text()).contains("Title");
        assertThat(parsed.text()).contains("This is a paragraph");
        assertThat(parsed.text()).doesNotContain("<html>");
        assertThat(parsed.text()).doesNotContain("<body>");
        assertThat(parsed.mimeType()).contains("text/html");
    }

    @Test
    void parse_csvFile_extractsContent() throws IOException {
        // Given
        String csvContent = "Name,Age,Department\nJohn,30,IT\nJane,25,HR";
        testFile = createTempFile("test.csv", csvContent);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isPresent();
        AttachmentParser.ParsedContent parsed = result.get();
        assertThat(parsed.text()).contains("Name");
        assertThat(parsed.text()).contains("John");
        assertThat(parsed.text()).contains("Jane");
    }

    @Test
    void parse_xmlFile_extractsContent() throws IOException {
        // Given
        String xmlContent = "<?xml version=\"1.0\"?><root><item>Value</item></root>";
        testFile = createTempFile("test.xml", xmlContent);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isPresent();
        AttachmentParser.ParsedContent parsed = result.get();
        assertThat(parsed.text()).contains("Value");
        assertThat(parsed.mimeType()).containsAnyOf("text/xml", "application/xml");
    }

    @Test
    void parse_jsonFile_extractsContent() throws IOException {
        // Given
        String jsonContent = "{\"name\": \"John\", \"age\": 30, \"city\": \"New York\"}";
        testFile = createTempFile("test.json", jsonContent);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isPresent();
        AttachmentParser.ParsedContent parsed = result.get();
        assertThat(parsed.text()).contains("John");
        assertThat(parsed.text()).contains("New York");
    }

    @Test
    void parse_emptyFile_returnsEmpty() throws IOException {
        // Given
        testFile = createTempFile("empty.txt", "");

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void parse_veryLargeFile_rejectsFile() throws IOException {
        // Given - Create a file larger than 50MB
        testFile = tempDir.resolve("large.txt");
        byte[] largeContent = new byte[51 * 1024 * 1024]; // 51MB
        Files.write(testFile, largeContent);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void parse_byteArray_extractsContent() {
        // Given
        String content = "This is test content from byte array.";
        byte[] data = content.getBytes(StandardCharsets.UTF_8);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(data, "test.txt");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("This is test content from byte array");
    }

    @Test
    void parse_inputStream_extractsContent() {
        // Given
        String content = "This is test content from input stream.";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(inputStream, "test.txt", "text/plain");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("This is test content from input stream");
    }

    @Test
    void extractText_plainTextFile_extractsContent() throws IOException {
        // Given
        String content = "Quick extraction test.";
        testFile = createTempFile("quick.txt", content);

        // When
        String extracted = parser.extractText(testFile);

        // Then
        assertThat(extracted).contains("Quick extraction test");
    }

    @Test
    void extractText_nonExistentFile_returnsEmptyString() {
        // Given
        Path nonExistent = tempDir.resolve("nonexistent.txt");

        // When
        String extracted = parser.extractText(nonExistent);

        // Then
        assertThat(extracted).isEmpty();
    }

    @Test
    void isSupportedMimeType_textPlain_returnsTrue() {
        // When
        boolean supported = parser.isSupportedMimeType("text/plain");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedMimeType_applicationPdf_returnsTrue() {
        // When
        boolean supported = parser.isSupportedMimeType("application/pdf");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedMimeType_textHtml_returnsTrue() {
        // When
        boolean supported = parser.isSupportedMimeType("text/html");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedMimeType_msWord_returnsTrue() {
        // When
        boolean supported = parser.isSupportedMimeType("application/msword");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedMimeType_docx_returnsTrue() {
        // When
        boolean supported = parser.isSupportedMimeType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedMimeType_anyTextType_returnsTrue() {
        // When
        boolean supported = parser.isSupportedMimeType("text/anything");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedMimeType_imageJpeg_returnsFalse() {
        // When
        boolean supported = parser.isSupportedMimeType("image/jpeg");

        // Then
        assertThat(supported).isFalse();
    }

    @Test
    void isSupportedMimeType_videoMp4_returnsFalse() {
        // When
        boolean supported = parser.isSupportedMimeType("video/mp4");

        // Then
        assertThat(supported).isFalse();
    }

    @Test
    void isSupportedMimeType_null_returnsFalse() {
        // When
        boolean supported = parser.isSupportedMimeType(null);

        // Then
        assertThat(supported).isFalse();
    }

    @Test
    void isSupportedMimeType_withCharset_returnsTrue() {
        // When
        boolean supported = parser.isSupportedMimeType("text/plain; charset=UTF-8");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedExtension_txt_returnsTrue() {
        // When
        boolean supported = parser.isSupportedExtension("document.txt");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedExtension_pdf_returnsTrue() {
        // When
        boolean supported = parser.isSupportedExtension("document.pdf");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedExtension_doc_returnsTrue() {
        // When
        boolean supported = parser.isSupportedExtension("document.doc");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedExtension_docx_returnsTrue() {
        // When
        boolean supported = parser.isSupportedExtension("document.docx");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedExtension_xlsx_returnsTrue() {
        // When
        boolean supported = parser.isSupportedExtension("spreadsheet.xlsx");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedExtension_pptx_returnsTrue() {
        // When
        boolean supported = parser.isSupportedExtension("presentation.pptx");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedExtension_html_returnsTrue() {
        // When
        boolean supported = parser.isSupportedExtension("page.html");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedExtension_csv_returnsTrue() {
        // When
        boolean supported = parser.isSupportedExtension("data.csv");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedExtension_odt_returnsTrue() {
        // When
        boolean supported = parser.isSupportedExtension("document.odt");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void isSupportedExtension_jpg_returnsFalse() {
        // When
        boolean supported = parser.isSupportedExtension("image.jpg");

        // Then
        assertThat(supported).isFalse();
    }

    @Test
    void isSupportedExtension_mp4_returnsFalse() {
        // When
        boolean supported = parser.isSupportedExtension("video.mp4");

        // Then
        assertThat(supported).isFalse();
    }

    @Test
    void isSupportedExtension_null_returnsFalse() {
        // When
        boolean supported = parser.isSupportedExtension(null);

        // Then
        assertThat(supported).isFalse();
    }

    @Test
    void isSupportedExtension_caseInsensitive_returnsTrue() {
        // When
        boolean supported = parser.isSupportedExtension("DOCUMENT.TXT");

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void parsedContent_length_returnsCorrectLength() throws IOException {
        // Given
        String content = "Test content";
        testFile = createTempFile("test.txt", content);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().length()).isGreaterThan(0);
    }

    @Test
    void parsedContent_isEmpty_returnsFalseForContent() throws IOException {
        // Given
        String content = "Test content";
        testFile = createTempFile("test.txt", content);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().isEmpty()).isFalse();
    }

    @Test
    void parse_whitespaceOnly_returnsEmpty() throws IOException {
        // Given
        String content = "   \n\n   \t\t   ";
        testFile = createTempFile("whitespace.txt", content);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void parse_normalizedLineEndings_convertsToUnix() throws IOException {
        // Given
        String content = "Line one\r\nLine two\r\nLine three";
        testFile = createTempFile("windows.txt", content);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isPresent();
        String extracted = result.get().text();
        assertThat(extracted).doesNotContain("\r\n");
        assertThat(extracted).contains("\n");
    }

    @Test
    void parse_excessiveWhitespace_normalized() throws IOException {
        // Given
        String content = "Text  with    excessive     spaces\n\n\n\nand    newlines";
        testFile = createTempFile("spaces.txt", content);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isPresent();
        String extracted = result.get().text();
        assertThat(extracted).doesNotContain("    ");
        assertThat(extracted).doesNotContain("\n\n\n");
    }

    @Test
    void parse_utf8Content_handlesCorrectly() throws IOException {
        // Given
        String content = "UTF-8 content: café, résumé, ñoño, 日本語";
        testFile = createTempFile("utf8.txt", content);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("café");
        assertThat(result.get().text()).contains("résumé");
    }

    @Test
    void parse_specialCharacters_preservesContent() throws IOException {
        // Given
        String content = "Special chars: @#$%^&*()_+-=[]{}|;':\",./<>?";
        testFile = createTempFile("special.txt", content);

        // When
        Optional<AttachmentParser.ParsedContent> result = parser.parse(testFile);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("Special chars");
    }

    @Test
    void shutdown_executorService_shutsDownCleanly() {
        // When
        parser.shutdown();

        // Then - No exceptions thrown
        assertThat(parser).isNotNull();
    }

    // Helper method to create temp files
    private Path createTempFile(String filename, String content) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
