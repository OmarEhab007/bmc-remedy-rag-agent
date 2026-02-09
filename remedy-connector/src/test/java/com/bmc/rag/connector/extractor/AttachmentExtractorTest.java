package com.bmc.rag.connector.extractor;

import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.model.AttachmentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AttachmentExtractor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttachmentExtractorTest {

    @Mock
    private ThreadLocalARContext mockArContext;

    @InjectMocks
    private AttachmentExtractor attachmentExtractor;

    @BeforeEach
    void setUp() {
        // Setup mock behavior if needed
    }

    @Test
    void constructor_createsInstance() {
        // Then
        assertThat(attachmentExtractor).isNotNull();
    }

    @Test
    void extractAttachment_validAttachment_returnsAttachmentInfo() {
        // Given
        String formName = "HPD:Help Desk";
        String entryId = "entry-123";
        int fieldId = 1000000600;

        AttachmentInfo mockInfo = AttachmentInfo.builder()
            .entryId(entryId)
            .fieldId(fieldId)
            .filename("document.pdf")
            .sizeBytes(1024L)
            .build();

        when(mockArContext.executeWithRetry(any())).thenReturn(Optional.of(mockInfo));

        // When
        Optional<AttachmentInfo> result = attachmentExtractor.extractAttachment(formName, entryId, fieldId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFilename()).isEqualTo("document.pdf");
    }

    @Test
    void extractAttachment_entryNotFound_returnsEmpty() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Optional.empty());

        // When
        Optional<AttachmentInfo> result = attachmentExtractor.extractAttachment("HPD:Help Desk", "entry-999", 1000000600);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void extractIncidentAttachments_validEntryId_returnsAttachmentList() {
        // Given
        String entryId = "entry-123";

        AttachmentInfo attachment1 = AttachmentInfo.builder()
            .entryId(entryId)
            .fieldId(1000000600)
            .filename("doc1.pdf")
            .sizeBytes(1024L)
            .source(AttachmentInfo.AttachmentSource.INCIDENT)
            .build();

        when(mockArContext.executeWithRetry(any())).thenReturn(Optional.of(attachment1));

        // When
        List<AttachmentInfo> results = attachmentExtractor.extractIncidentAttachments(entryId);

        // Then
        assertThat(results).isNotEmpty();
    }

    @Test
    void extractIncidentAttachments_noAttachments_returnsEmptyList() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Optional.empty());

        // When
        List<AttachmentInfo> results = attachmentExtractor.extractIncidentAttachments("entry-123");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void extractWorkOrderAttachments_validEntryId_returnsAttachmentList() {
        // Given
        String entryId = "entry-123";

        AttachmentInfo attachment1 = AttachmentInfo.builder()
            .entryId(entryId)
            .fieldId(1000000600)
            .filename("workorder-doc.pdf")
            .sizeBytes(2048L)
            .source(AttachmentInfo.AttachmentSource.WORK_ORDER)
            .build();

        when(mockArContext.executeWithRetry(any())).thenReturn(Optional.of(attachment1));

        // When
        List<AttachmentInfo> results = attachmentExtractor.extractWorkOrderAttachments(entryId);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getSource()).isEqualTo(AttachmentInfo.AttachmentSource.WORK_ORDER);
    }

    @Test
    void extractChangeRequestAttachments_validEntryId_returnsAttachmentList() {
        // Given
        String entryId = "entry-123";

        AttachmentInfo attachment1 = AttachmentInfo.builder()
            .entryId(entryId)
            .fieldId(1000000600)
            .filename("change-plan.docx")
            .sizeBytes(3072L)
            .source(AttachmentInfo.AttachmentSource.CHANGE_REQUEST)
            .build();

        when(mockArContext.executeWithRetry(any())).thenReturn(Optional.of(attachment1));

        // When
        List<AttachmentInfo> results = attachmentExtractor.extractChangeRequestAttachments(entryId);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getSource()).isEqualTo(AttachmentInfo.AttachmentSource.CHANGE_REQUEST);
    }

    @Test
    void extractWorkLogAttachments_validParameters_returnsAttachmentList() {
        // Given
        String formName = "HPD:WorkLog";
        String entryId = "worklog-123";
        AttachmentInfo.AttachmentSource source = AttachmentInfo.AttachmentSource.INCIDENT;

        AttachmentInfo attachment1 = AttachmentInfo.builder()
            .entryId(entryId)
            .fieldId(1000000568)
            .filename("worklog-screenshot.png")
            .sizeBytes(512L)
            .source(source)
            .build();

        when(mockArContext.executeWithRetry(any())).thenReturn(Optional.of(attachment1));

        // When
        List<AttachmentInfo> results = attachmentExtractor.extractWorkLogAttachments(formName, entryId, source);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getSource()).isEqualTo(source);
    }

    @Test
    void isSupported_pdfFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("document.pdf");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_docxFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("report.docx");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_txtFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("notes.txt");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_imageFile_returnsFalse() {
        // When
        boolean result = attachmentExtractor.isSupported("image.jpg");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupported_exeFile_returnsFalse() {
        // When
        boolean result = attachmentExtractor.isSupported("program.exe");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupported_nullFilename_returnsFalse() {
        // When
        boolean result = attachmentExtractor.isSupported(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupported_xlsxFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("spreadsheet.xlsx");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_pptxFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("presentation.pptx");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void extractAttachment_largeFile_handlesGracefully() {
        // Given - Mock a large file that exceeds size limit
        AttachmentInfo largeFileInfo = AttachmentInfo.builder()
            .entryId("entry-123")
            .fieldId(1000000600)
            .filename("large-file.pdf")
            .sizeBytes(60 * 1024 * 1024L) // 60MB - exceeds 50MB limit
            .build();

        when(mockArContext.executeWithRetry(any())).thenReturn(Optional.of(largeFileInfo));

        // When
        Optional<AttachmentInfo> result = attachmentExtractor.extractAttachment("HPD:Help Desk", "entry-123", 1000000600);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSizeBytes()).isGreaterThan(50 * 1024 * 1024L);
    }

    @Test
    void isSupported_rtfFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("document.rtf");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_csvFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("data.csv");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_odtFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("document.odt");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_odsFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("spreadsheet.ods");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_htmlFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("page.html");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_htmFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("page.htm");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_xmlFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("config.xml");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_docFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("old-document.doc");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_xlsFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("old-spreadsheet.xls");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_pptFile_returnsTrue() {
        // When
        boolean result = attachmentExtractor.isSupported("old-presentation.ppt");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_zipFile_returnsFalse() {
        // When
        boolean result = attachmentExtractor.isSupported("archive.zip");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupported_mp3File_returnsFalse() {
        // When
        boolean result = attachmentExtractor.isSupported("song.mp3");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupported_mp4File_returnsFalse() {
        // When
        boolean result = attachmentExtractor.isSupported("video.mp4");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupported_upperCaseExtension_returnsCorrectly() {
        // When
        boolean pdfResult = attachmentExtractor.isSupported("DOCUMENT.PDF");
        boolean exeResult = attachmentExtractor.isSupported("PROGRAM.EXE");

        // Then
        assertThat(pdfResult).isTrue();
        assertThat(exeResult).isFalse();
    }

    @Test
    void isSupported_mixedCaseExtension_returnsCorrectly() {
        // When
        boolean result = attachmentExtractor.isSupported("Report.DocX");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupported_noExtension_returnsFalse() {
        // When
        boolean result = attachmentExtractor.isSupported("README");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupported_emptyFilename_returnsFalse() {
        // When
        boolean result = attachmentExtractor.isSupported("");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupported_filenameWithDot_returnsFalse() {
        // When
        boolean result = attachmentExtractor.isSupported("file.");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupported_multipleExtensions_checksLast() {
        // When
        boolean result = attachmentExtractor.isSupported("archive.tar.gz");

        // Then
        assertThat(result).isFalse(); // gz is not supported
    }

    @Test
    void extractIncidentAttachments_multipleFieldsWithSomeEmpty_returnsOnlyValid() {
        // Given
        AttachmentInfo attachment1 = AttachmentInfo.builder()
            .entryId("entry-123")
            .fieldId(1000000600)
            .filename("doc1.pdf")
            .sizeBytes(1024L)
            .source(AttachmentInfo.AttachmentSource.INCIDENT)
            .build();

        // First field has attachment, second and third are empty
        when(mockArContext.executeWithRetry(any()))
            .thenReturn(Optional.of(attachment1))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        // When
        List<AttachmentInfo> results = attachmentExtractor.extractIncidentAttachments("entry-123");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFilename()).isEqualTo("doc1.pdf");
    }

    @Test
    void extractWorkOrderAttachments_noAttachments_returnsEmptyList() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Optional.empty());

        // When
        List<AttachmentInfo> results = attachmentExtractor.extractWorkOrderAttachments("entry-123");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void extractChangeRequestAttachments_noAttachments_returnsEmptyList() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(Optional.empty());

        // When
        List<AttachmentInfo> results = attachmentExtractor.extractChangeRequestAttachments("entry-123");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void extractWorkLogAttachments_validSource_setsSourceCorrectly() {
        // Given
        String formName = "HPD:WorkLog";
        String entryId = "worklog-123";
        AttachmentInfo.AttachmentSource source = AttachmentInfo.AttachmentSource.WORK_ORDER;

        AttachmentInfo attachment = AttachmentInfo.builder()
            .entryId(entryId)
            .fieldId(1000000568)
            .filename("worklog.pdf")
            .sizeBytes(512L)
            .source(source)
            .build();

        when(mockArContext.executeWithRetry(any())).thenReturn(Optional.of(attachment));

        // When
        List<AttachmentInfo> results = attachmentExtractor.extractWorkLogAttachments(formName, entryId, source);

        // Then
        assertThat(results).hasSize(3); // Checks 3 field IDs
        assertThat(results.get(0).getSource()).isEqualTo(source);
    }

}
