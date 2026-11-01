package com.example.usbwebcam;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

public class AboutCopyTest {
    @Test
    public void bothLocalesCreditCreatorAndPurpose() throws Exception {
        String english = new String(Files.readAllBytes(Paths.get("src/main/res/values/strings.xml")),
                StandardCharsets.UTF_8);
        String spanish = new String(Files.readAllBytes(Paths.get("src/main/res/values-es/strings.xml")),
                StandardCharsets.UTF_8);
        assertTrue(english.contains("name=\"about_message\""));
        assertTrue(spanish.contains("name=\"about_message\""));
        assertTrue(english.contains("menagreen"));
        assertTrue(spanish.contains("menagreen"));
        assertTrue(english.contains("content creators"));
        assertTrue(english.contains("phone camera"));
        assertTrue(spanish.contains("creadores de contenido"));
        assertTrue(spanish.contains("cámara de su celular"));
    }
}
