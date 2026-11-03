package com.example.usbwebcam;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

public class HelpCopyTest {
    @Test public void helpExplainsAdbFreeRoutesInBothLocales() throws Exception {
        String english = new String(Files.readAllBytes(Paths.get("src/main/res/values/strings.xml")),
                StandardCharsets.UTF_8);
        String spanish = new String(Files.readAllBytes(Paths.get("src/main/res/values-es/strings.xml")),
                StandardCharsets.UTF_8);
        assertTrue(english.contains("Wi-Fi or USB tethering"));
        assertTrue(english.contains("ADB is optional"));
        assertTrue(spanish.contains("Wi-Fi o conexión compartida por USB"));
        assertTrue(spanish.contains("ADB es opcional"));
    }
}
