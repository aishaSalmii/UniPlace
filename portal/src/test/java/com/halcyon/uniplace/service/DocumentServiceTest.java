package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.DocumentType;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.StudentDocument;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentServiceTest extends IntegrationTest {

    @Autowired DocumentService service;

    private static MockMultipartFile pdf(String name, int size) {
        byte[] bytes = new byte[size];
        byte[] head = "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(head, 0, bytes, 0, head.length);
        return new MockMultipartFile("file", name, "application/pdf", bytes);
    }

    @Test
    void validCvIsStored() {
        Student s = student("3.00", List.of("Excel"));
        StudentDocument d = service.upload(s, DocumentType.CV, pdf("my cv.pdf", 50_000), null);
        assertEquals("my cv.pdf", d.getOriginalName());
        assertEquals(50_000, d.getSizeBytes());
    }

    @Test
    void cvLargerThanTwoMegabytesIsRefused() {
        Student s = student("3.00", List.of("Excel"));
        assertThrows(BusinessRuleException.class,
                () -> service.upload(s, DocumentType.CV, pdf("big.pdf", 2 * 1024 * 1024 + 1), null));
    }

    @Test
    void fileThatIsNotAPdfIsRefused() {
        Student s = student("3.00", List.of("Excel"));
        MockMultipartFile fake = new MockMultipartFile("file", "cv.pdf", "application/pdf", "hello".getBytes());
        assertThrows(BusinessRuleException.class, () -> service.upload(s, DocumentType.CV, fake, null));
    }

    @Test
    void transcriptNeedsAnIssueDate() {
        Student s = student("3.00", List.of("Excel"));
        assertThrows(BusinessRuleException.class,
                () -> service.upload(s, DocumentType.TRANSCRIPT, pdf("t.pdf", 1000), null));
    }

    @Test
    void newUploadReplacesThePreviousDocument() {
        Student s = student("3.00", List.of("Excel"));
        service.upload(s, DocumentType.CV, pdf("first.pdf", 1000), null);
        service.upload(s, DocumentType.CV, pdf("second.pdf", 1000), null);

        List<StudentDocument> docs = documents.findByStudent(s);
        assertEquals(1, docs.size());
        assertEquals("second.pdf", docs.get(0).getOriginalName());
    }

    @Test
    void fileNamesAreSanitised() {
        assertEquals("passwd", DocumentService.sanitise("../../etc/passwd"));
        assertEquals("a_b_.pdf", DocumentService.sanitise("a<b>.pdf"));
        assertEquals("document.pdf", DocumentService.sanitise(".hidden"));
    }
}
