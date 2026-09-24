package com.qareporting.resource;

import com.qareporting.entity.Attachment;
import com.qareporting.entity.Defect;
import com.qareporting.security.CurrentUser;
import com.qareporting.service.AttachmentService;
import com.qareporting.service.DefectService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/defects/{defectId}/attachments")
public class AttachmentResource {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "application/pdf", "text/plain",
            "application/zip", "application/octet-stream");
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB, same ceiling as the Laravel version

    @Inject
    AttachmentService attachmentService;

    @Inject
    DefectService defectService;

    @Inject
    CurrentUser currentUser;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@PathParam("defectId") Long defectId, MultipartFormDataInput input) {
        Defect defect = defectService.find(defectId);
        if (defect == null) {
            return Response.status(404).build();
        }

        Map<String, List<InputPart>> parts = input.getFormDataMap();
        List<InputPart> fileParts = parts.get("file");
        if (fileParts == null || fileParts.isEmpty()) {
            return Response.status(422).entity("{\"message\":\"Aucun fichier fourni.\"}").build();
        }

        InputPart filePart = fileParts.get(0);
        MediaType fullMediaType = filePart.getMediaType();
        // Compare only type/subtype — a client-supplied charset parameter (e.g.
        // "text/plain;charset=us-ascii") must not fail the whitelist check.
        String mimeType = fullMediaType.getType() + "/" + fullMediaType.getSubtype();
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            return Response.status(422).entity("{\"message\":\"Type de fichier non autorisé: " + mimeType + "\"}").build();
        }

        String filename = extractFilename(filePart).orElse("fichier");

        byte[] content;
        try (InputStream stream = filePart.getBody(InputStream.class, null)) {
            content = stream.readAllBytes();
        } catch (IOException e) {
            return Response.status(500).entity("{\"message\":\"Erreur de lecture du fichier.\"}").build();
        }

        if (content.length > MAX_SIZE_BYTES) {
            return Response.status(422).entity("{\"message\":\"Le fichier dépasse la taille maximale de 10 Mo.\"}").build();
        }

        Attachment attachment = attachmentService.store(defect, currentUser.get(), filename, mimeType, content);
        return Response.status(201).entity(attachment).build();
    }

    @DELETE
    @Path("/{attachmentId}")
    public Response destroy(@PathParam("defectId") Long defectId, @PathParam("attachmentId") Long attachmentId) {
        return attachmentService.delete(attachmentId) ? Response.noContent().build() : Response.status(404).build();
    }

    private java.util.Optional<String> extractFilename(InputPart part) {
        String contentDisposition = part.getHeaders().getFirst("Content-Disposition");
        if (contentDisposition == null) {
            return java.util.Optional.empty();
        }
        for (String segment : contentDisposition.split(";")) {
            segment = segment.trim();
            if (segment.startsWith("filename")) {
                String value = segment.substring(segment.indexOf('=') + 1).trim();
                return java.util.Optional.of(value.replace("\"", ""));
            }
        }
        return java.util.Optional.empty();
    }
}
