package com.qareporting.service;

import com.qareporting.entity.Attachment;
import com.qareporting.entity.Defect;
import com.qareporting.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Stores attachment files on disk under WildFly's own data directory
 * (outside the deployed WAR, so they survive a redeploy) — the Jakarta EE
 * counterpart of Laravel's local filesystem disk for defect attachments.
 */
@ApplicationScoped
public class AttachmentService {

    @PersistenceContext(unitName = "qaReportingJ2eePU")
    EntityManager em;

    private Path storageRoot() {
        String dataDir = System.getProperty("jboss.server.data.dir", System.getProperty("java.io.tmpdir"));
        Path root = Paths.get(dataDir, "qa-reporting-j2ee", "attachments");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return root;
    }

    @Transactional
    public Attachment store(Defect defect, User uploadedBy, String originalFilename, String mimeType, byte[] content) {
        String storedName = UUID.randomUUID() + "-" + originalFilename;
        Path target = storageRoot().resolve(storedName);
        try {
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Attachment attachment = new Attachment();
        attachment.setDefect(defect);
        attachment.setUploadedBy(uploadedBy);
        attachment.setFilename(originalFilename);
        attachment.setPath(target.toString());
        attachment.setMimeType(mimeType);
        attachment.setSize(content.length);
        em.persist(attachment);
        return attachment;
    }

    @Transactional
    public boolean delete(Long attachmentId) {
        Attachment attachment = em.find(Attachment.class, attachmentId);
        if (attachment == null) {
            return false;
        }
        try {
            Files.deleteIfExists(Paths.get(attachment.getPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        em.remove(attachment);
        return true;
    }
}
