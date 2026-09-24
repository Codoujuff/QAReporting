package com.qareporting.service;

import com.qareporting.entity.Role;
import java.util.Set;
import java.util.List;
import jakarta.inject.Inject;
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

    /** Types acceptés : captures, vidéos courtes, PDF, journaux texte, archives. */
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "video/mp4",
            "application/pdf", "text/plain", "text/csv", "application/json",
            "application/zip", "application/x-zip-compressed", "application/octet-stream");
    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10 Mo, même plafond que la version Laravel

    @Inject
    AuditService audit;

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
        String safeName = safeFilename(originalFilename);
        String storedName = UUID.randomUUID() + "-" + safeName;
        Path target = storageRoot().resolve(storedName);
        try {
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Attachment attachment = new Attachment();
        attachment.setDefect(defect);
        attachment.setUploadedBy(uploadedBy);
        attachment.setFilename(safeName);
        attachment.setPath(target.toString());
        attachment.setMimeType(mimeType);
        attachment.setSize(content.length);
        em.persist(attachment);
        audit.record(AuditService.CREATED, attachment);
        return attachment;
    }

    public List<Attachment> listFor(Defect defect) {
        return em.createQuery("SELECT a FROM Attachment a WHERE a.defect = :defect ORDER BY a.createdAt DESC",
                        Attachment.class)
                .setParameter("defect", defect)
                .getResultList();
    }

    public Attachment find(Long attachmentId) {
        return attachmentId == null ? null : em.find(Attachment.class, attachmentId);
    }

    public byte[] read(Attachment attachment) {
        try {
            return Files.readAllBytes(Paths.get(attachment.getPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Une pièce jointe se retire par la personne qui l'a déposée, ou par l'admin. */
    public boolean canDelete(User viewer, Attachment attachment) {
        if (viewer == null || attachment == null) {
            return false;
        }
        boolean admin = viewer.getRole() != null && Role.ADMIN.equals(viewer.getRole().getName());
        return admin || (attachment.getUploadedBy() != null
                && attachment.getUploadedBy().getId().equals(viewer.getId()));
    }

    /**
     * Nom d'origine réduit à sa dernière partie et à des caractères sûrs : sans cela, un nom
     * comme « ../../x » ferait écrire le fichier hors du dossier des pièces jointes.
     */
    static String safeFilename(String original) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^\\p{L}\\p{N}._ -]", "_").replaceAll("^[. ]+", "").strip();
        if (name.length() > 150) {
            name = name.substring(name.length() - 150);
        }
        return name.isEmpty() ? "fichier" : name;
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
        audit.record(AuditService.DELETED, attachment);
        em.remove(attachment);
        return true;
    }
}
