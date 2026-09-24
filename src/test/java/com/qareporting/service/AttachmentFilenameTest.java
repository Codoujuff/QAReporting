package com.qareporting.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Le nom de fichier envoyé par le client ne doit jamais sortir du dossier des pièces jointes. */
class AttachmentFilenameTest {

    @Test
    void pathTraversalIsReducedToTheBareName() {
        assertEquals("passwd", AttachmentService.safeFilename("../../etc/passwd"));
        assertEquals("evil.txt", AttachmentService.safeFilename("..\\..\\evil.txt"));
    }

    @Test
    void accentsAreKeptButControlCharactersAreNot() {
        assertEquals("capture écran 2.png", AttachmentService.safeFilename("capture écran 2.png"));
        assertEquals("a_b.log", AttachmentService.safeFilename("a:b.log"));
    }

    @Test
    void emptyOrDotOnlyNamesGetADefault() {
        assertEquals("fichier", AttachmentService.safeFilename(null));
        assertEquals("fichier", AttachmentService.safeFilename(".."));
    }
}
