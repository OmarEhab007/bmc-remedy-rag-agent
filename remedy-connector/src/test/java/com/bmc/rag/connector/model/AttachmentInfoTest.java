package com.bmc.rag.connector.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AttachmentInfo model class.
 */
@DisplayName("AttachmentInfo")
class AttachmentInfoTest {

    @Nested
    @DisplayName("isTextParseable")
    class IsTextParseableTests {

        @Test
        @DisplayName("returns true for PDF files")
        void isTextParseable_pdf() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("document.pdf")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for DOC files")
        void isTextParseable_doc() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("document.doc")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for DOCX files")
        void isTextParseable_docx() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("document.docx")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for TXT files")
        void isTextParseable_txt() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("notes.txt")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for RTF files")
        void isTextParseable_rtf() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("document.rtf")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for XLS files")
        void isTextParseable_xls() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("spreadsheet.xls")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for XLSX files")
        void isTextParseable_xlsx() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("spreadsheet.xlsx")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for PPT files")
        void isTextParseable_ppt() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("presentation.ppt")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for PPTX files")
        void isTextParseable_pptx() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("presentation.pptx")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for HTML files")
        void isTextParseable_html() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("page.html")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for HTM files")
        void isTextParseable_htm() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("page.htm")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for XML files")
        void isTextParseable_xml() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("data.xml")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns true for CSV files")
        void isTextParseable_csv() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("data.csv")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("returns false for PNG files")
        void isTextParseable_png() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("screenshot.png")
                    .build();

            assertThat(attachment.isTextParseable()).isFalse();
        }

        @Test
        @DisplayName("returns false for EXE files")
        void isTextParseable_exe() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("installer.exe")
                    .build();

            assertThat(attachment.isTextParseable()).isFalse();
        }

        @Test
        @DisplayName("returns false for null filename")
        void isTextParseable_nullFilename() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .build();

            assertThat(attachment.isTextParseable()).isFalse();
        }

        @Test
        @DisplayName("handles uppercase extensions")
        void isTextParseable_uppercaseExtension() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("DOCUMENT.PDF")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }

        @Test
        @DisplayName("handles mixed case extensions")
        void isTextParseable_mixedCaseExtension() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("Document.PdF")
                    .build();

            assertThat(attachment.isTextParseable()).isTrue();
        }
    }

    @Nested
    @DisplayName("isImage")
    class IsImageTests {

        @Test
        @DisplayName("returns true for PNG files")
        void isImage_png() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("screenshot.png")
                    .build();

            assertThat(attachment.isImage()).isTrue();
        }

        @Test
        @DisplayName("returns true for JPG files")
        void isImage_jpg() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("photo.jpg")
                    .build();

            assertThat(attachment.isImage()).isTrue();
        }

        @Test
        @DisplayName("returns true for JPEG files")
        void isImage_jpeg() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("photo.jpeg")
                    .build();

            assertThat(attachment.isImage()).isTrue();
        }

        @Test
        @DisplayName("returns true for GIF files")
        void isImage_gif() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("animation.gif")
                    .build();

            assertThat(attachment.isImage()).isTrue();
        }

        @Test
        @DisplayName("returns true for BMP files")
        void isImage_bmp() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("bitmap.bmp")
                    .build();

            assertThat(attachment.isImage()).isTrue();
        }

        @Test
        @DisplayName("returns true for TIFF files")
        void isImage_tiff() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("scan.tiff")
                    .build();

            assertThat(attachment.isImage()).isTrue();
        }

        @Test
        @DisplayName("returns false for PDF files")
        void isImage_pdf() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("document.pdf")
                    .build();

            assertThat(attachment.isImage()).isFalse();
        }

        @Test
        @DisplayName("returns false for null filename")
        void isImage_nullFilename() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .build();

            assertThat(attachment.isImage()).isFalse();
        }

        @Test
        @DisplayName("handles uppercase extensions")
        void isImage_uppercaseExtension() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("PHOTO.PNG")
                    .build();

            assertThat(attachment.isImage()).isTrue();
        }
    }

    @Nested
    @DisplayName("getExtension")
    class GetExtensionTests {

        @Test
        @DisplayName("extracts extension from filename")
        void getExtension_normalFilename() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("document.pdf")
                    .build();

            assertThat(attachment.getExtension()).isEqualTo("pdf");
        }

        @Test
        @DisplayName("returns lowercase extension")
        void getExtension_lowercaseExtension() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("DOCUMENT.PDF")
                    .build();

            assertThat(attachment.getExtension()).isEqualTo("pdf");
        }

        @Test
        @DisplayName("handles multiple dots in filename")
        void getExtension_multipleDotsInFilename() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("my.backup.file.tar.gz")
                    .build();

            assertThat(attachment.getExtension()).isEqualTo("gz");
        }

        @Test
        @DisplayName("returns null when no extension")
        void getExtension_noExtension() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("README")
                    .build();

            assertThat(attachment.getExtension()).isNull();
        }

        @Test
        @DisplayName("returns null when filename is null")
        void getExtension_nullFilename() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .build();

            assertThat(attachment.getExtension()).isNull();
        }

        @Test
        @DisplayName("returns null when dot at start of filename")
        void getExtension_dotAtStart() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename(".gitignore")
                    .build();

            assertThat(attachment.getExtension()).isNull();
        }
    }

    @Nested
    @DisplayName("getHumanReadableSize")
    class GetHumanReadableSizeTests {

        @Test
        @DisplayName("formats bytes correctly")
        void getHumanReadableSize_bytes() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(512)
                    .build();

            assertThat(attachment.getHumanReadableSize()).isEqualTo("512 B");
        }

        @Test
        @DisplayName("formats kilobytes correctly")
        void getHumanReadableSize_kilobytes() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(1536) // 1.5 KB
                    .build();

            assertThat(attachment.getHumanReadableSize()).isEqualTo("1.5 KB");
        }

        @Test
        @DisplayName("formats megabytes correctly")
        void getHumanReadableSize_megabytes() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(2621440) // 2.5 MB
                    .build();

            assertThat(attachment.getHumanReadableSize()).isEqualTo("2.5 MB");
        }

        @Test
        @DisplayName("formats gigabytes correctly")
        void getHumanReadableSize_gigabytes() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(3221225472L) // 3.0 GB
                    .build();

            assertThat(attachment.getHumanReadableSize()).isEqualTo("3.0 GB");
        }

        @Test
        @DisplayName("handles boundary at 1 KB")
        void getHumanReadableSize_boundaryKB() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(1023)
                    .build();

            assertThat(attachment.getHumanReadableSize()).isEqualTo("1023 B");

            attachment = AttachmentInfo.builder()
                    .sizeBytes(1024)
                    .build();

            assertThat(attachment.getHumanReadableSize()).isEqualTo("1.0 KB");
        }

        @Test
        @DisplayName("handles boundary at 1 MB")
        void getHumanReadableSize_boundaryMB() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(1048575) // 1023.999 KB
                    .build();

            assertThat(attachment.getHumanReadableSize()).contains("KB");

            attachment = AttachmentInfo.builder()
                    .sizeBytes(1048576) // 1 MB
                    .build();

            assertThat(attachment.getHumanReadableSize()).isEqualTo("1.0 MB");
        }

        @Test
        @DisplayName("handles zero bytes")
        void getHumanReadableSize_zeroBytes() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(0)
                    .build();

            assertThat(attachment.getHumanReadableSize()).isEqualTo("0 B");
        }
    }

    @Nested
    @DisplayName("isWithinSizeLimit")
    class IsWithinSizeLimitTests {

        @Test
        @DisplayName("returns true for file within default limit")
        void isWithinSizeLimit_withinDefaultLimit() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(10 * 1024 * 1024) // 10 MB
                    .build();

            assertThat(attachment.isWithinSizeLimit()).isTrue();
        }

        @Test
        @DisplayName("returns false for file exceeding default limit")
        void isWithinSizeLimit_exceedsDefaultLimit() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(100 * 1024 * 1024) // 100 MB
                    .build();

            assertThat(attachment.isWithinSizeLimit()).isFalse();
        }

        @Test
        @DisplayName("returns true for file exactly at default limit")
        void isWithinSizeLimit_exactlyAtDefaultLimit() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(50 * 1024 * 1024) // 50 MB
                    .build();

            assertThat(attachment.isWithinSizeLimit()).isTrue();
        }

        @Test
        @DisplayName("returns true for file within custom limit")
        void isWithinSizeLimit_withinCustomLimit() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(5 * 1024 * 1024) // 5 MB
                    .build();

            assertThat(attachment.isWithinSizeLimit(10 * 1024 * 1024)).isTrue();
        }

        @Test
        @DisplayName("returns false for file exceeding custom limit")
        void isWithinSizeLimit_exceedsCustomLimit() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(15 * 1024 * 1024) // 15 MB
                    .build();

            assertThat(attachment.isWithinSizeLimit(10 * 1024 * 1024)).isFalse();
        }

        @Test
        @DisplayName("returns true for file exactly at custom limit")
        void isWithinSizeLimit_exactlyAtCustomLimit() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .sizeBytes(10 * 1024 * 1024) // 10 MB
                    .build();

            assertThat(attachment.isWithinSizeLimit(10 * 1024 * 1024)).isTrue();
        }
    }

    @Nested
    @DisplayName("AttachmentSource Enum")
    class AttachmentSourceTests {

        @Test
        @DisplayName("all enum values are accessible")
        void attachmentSource_allValues() {
            AttachmentInfo.AttachmentSource[] values = AttachmentInfo.AttachmentSource.values();

            assertThat(values)
                    .hasSize(7)
                    .contains(
                            AttachmentInfo.AttachmentSource.INCIDENT,
                            AttachmentInfo.AttachmentSource.INCIDENT_WORK_LOG,
                            AttachmentInfo.AttachmentSource.WORK_ORDER,
                            AttachmentInfo.AttachmentSource.WORK_ORDER_WORK_LOG,
                            AttachmentInfo.AttachmentSource.CHANGE_REQUEST,
                            AttachmentInfo.AttachmentSource.CHANGE_WORK_LOG,
                            AttachmentInfo.AttachmentSource.KNOWLEDGE_ARTICLE
                    );
        }
    }

    @Nested
    @DisplayName("Complete Object Creation")
    class CompleteObjectTests {

        @Test
        @DisplayName("can create fully populated attachment")
        void builder_createFullyPopulatedAttachment() {
            Instant now = Instant.now();

            AttachmentInfo attachment = AttachmentInfo.builder()
                    .entryId("000000000000500")
                    .fieldId(536870913)
                    .filename("troubleshooting_guide.pdf")
                    .sizeBytes(2621440) // 2.5 MB
                    .createDate(now)
                    .source(AttachmentInfo.AttachmentSource.INCIDENT)
                    .parentRecordId("INC000000000001")
                    .extractedText("Troubleshooting steps...")
                    .contentType("application/pdf")
                    .parsed(true)
                    .build();

            assertThat(attachment.getFilename()).isEqualTo("troubleshooting_guide.pdf");
            assertThat(attachment.getExtension()).isEqualTo("pdf");
            assertThat(attachment.isTextParseable()).isTrue();
            assertThat(attachment.isImage()).isFalse();
            assertThat(attachment.getHumanReadableSize()).isEqualTo("2.5 MB");
            assertThat(attachment.isWithinSizeLimit()).isTrue();
            assertThat(attachment.getSource()).isEqualTo(AttachmentInfo.AttachmentSource.INCIDENT);
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods")
    class LombokGeneratedMethodsTests {

        @Test
        @DisplayName("equals returns true for same object")
        void equals_sameObject_returnsTrue() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("test.pdf")
                    .sizeBytes(1024)
                    .build();

            assertThat(attachment).isEqualTo(attachment);
        }

        @Test
        @DisplayName("equals returns true for same values")
        void equals_sameValues_returnsTrue() {
            AttachmentInfo att1 = AttachmentInfo.builder()
                    .filename("test.pdf")
                    .sizeBytes(1024)
                    .fieldId(123)
                    .build();

            AttachmentInfo att2 = AttachmentInfo.builder()
                    .filename("test.pdf")
                    .sizeBytes(1024)
                    .fieldId(123)
                    .build();

            assertThat(att1).isEqualTo(att2);
            assertThat(att1.hashCode()).isEqualTo(att2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different values")
        void equals_differentFilename_returnsFalse() {
            AttachmentInfo att1 = AttachmentInfo.builder()
                    .filename("test1.pdf")
                    .build();

            AttachmentInfo att2 = AttachmentInfo.builder()
                    .filename("test2.pdf")
                    .build();

            assertThat(att1).isNotEqualTo(att2);
        }

        @Test
        @DisplayName("toString includes key fields")
        void toString_includesKeyFields() {
            AttachmentInfo attachment = AttachmentInfo.builder()
                    .filename("test.pdf")
                    .sizeBytes(1024)
                    .fieldId(123)
                    .build();

            String result = attachment.toString();

            assertThat(result).contains("test.pdf");
            assertThat(result).contains("sizeBytes=1024");
            assertThat(result).contains("fieldId=123");
        }

        @Test
        @DisplayName("no-args constructor creates object")
        void noArgsConstructor_createsObject() {
            AttachmentInfo attachment = new AttachmentInfo();
            assertThat(attachment).isNotNull();
        }

        @Test
        @DisplayName("all-args constructor sets all fields")
        void allArgsConstructor_setsAllFields() {
            Instant now = Instant.now();
            AttachmentInfo att = new AttachmentInfo(
                "entry1", 123, "test.pdf", 1024, now,
                AttachmentInfo.AttachmentSource.INCIDENT, "INC001",
                "extracted", "application/pdf", true
            );

            assertThat(att.getEntryId()).isEqualTo("entry1");
            assertThat(att.getFilename()).isEqualTo("test.pdf");
        }

        @Test
        @DisplayName("enum valueOf works correctly")
        void enumValueOf_worksCorrectly() {
            AttachmentInfo.AttachmentSource source = AttachmentInfo.AttachmentSource.valueOf("INCIDENT");
            assertThat(source).isEqualTo(AttachmentInfo.AttachmentSource.INCIDENT);
        }
    }
}
