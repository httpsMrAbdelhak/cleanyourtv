package com.cleanyourtv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Guards the promises this launcher makes about itself: it asks for no permission, runs no accessibility
 * service, is the Android home screen candidate, and keeps its package name. Gradle runs unit tests from
 * the module folder (app/), which is where these paths start.
 */
public class ManifestPolicyTest {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";

    private static File module() {
        File here = new File("src/main").getAbsoluteFile();
        if (!here.isDirectory()) here = new File("app/src/main").getAbsoluteFile();
        assertTrue("run the tests from the app/ module folder", here.isDirectory());
        return here.getParentFile().getParentFile();
    }

    private static Document manifest() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new File(module(), "src/main/AndroidManifest.xml"));
    }

    private static String text(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void asksForNoPermission() throws Exception {
        Document doc = manifest();
        for (String tag : new String[] {"uses-permission", "uses-permission-sdk-23", "permission"}) {
            assertEquals(tag, 0, doc.getElementsByTagName(tag).getLength());
        }
    }

    @Test public void declaresNoServiceReceiverOrProvider() throws Exception {
        Document doc = manifest();
        for (String tag : new String[] {"service", "receiver", "provider"}) {
            assertEquals(tag + " is not needed: no accessibility, boot or notification hooks",
                    0, doc.getElementsByTagName(tag).getLength());
        }
        // Comments may name what is absent; only the markup counts.
        String raw = text(new File(module(), "src/main/AndroidManifest.xml")).replaceAll("(?s)<!--.*?-->", "");
        assertFalse(raw.contains("BIND_ACCESSIBILITY_SERVICE"));
        assertFalse(raw.contains("WRITE_SECURE_SETTINGS"));
        assertFalse(raw.contains("QUERY_ALL_PACKAGES"));
        assertFalse(raw.contains("INTERNET"));
    }

    @Test public void homeActivityIsAHomeCandidateAndListedInTheAppRow() throws Exception {
        NodeList activities = manifest().getElementsByTagName("activity");
        boolean home = false;
        boolean leanback = false;
        for (int i = 0; i < activities.getLength(); i++) {
            Element activity = (Element) activities.item(i);
            if (!".HomeActivity".equals(activity.getAttributeNS(ANDROID, "name"))) continue;
            assertEquals("true", activity.getAttributeNS(ANDROID, "exported"));
            NodeList categories = activity.getElementsByTagName("category");
            for (int c = 0; c < categories.getLength(); c++) {
                String name = ((Element) categories.item(c)).getAttributeNS(ANDROID, "name");
                if ("android.intent.category.HOME".equals(name)) home = true;
                if ("android.intent.category.LEANBACK_LAUNCHER".equals(name)) leanback = true;
            }
        }
        assertTrue("HomeActivity must answer the HOME intent", home);
        assertTrue("HomeActivity must be listed in the TV's app row", leanback);
    }

    @Test public void onlyHomeActivityIsExported() throws Exception {
        NodeList activities = manifest().getElementsByTagName("activity");
        Set<String> exported = new HashSet<>();
        for (int i = 0; i < activities.getLength(); i++) {
            Element activity = (Element) activities.item(i);
            if ("true".equals(activity.getAttributeNS(ANDROID, "exported"))) {
                exported.add(activity.getAttributeNS(ANDROID, "name"));
            }
        }
        assertEquals(java.util.Collections.singleton(".HomeActivity"), exported);
    }

    @Test public void tvIsDeclaredWithoutRequiringATouchscreen() throws Exception {
        NodeList features = manifest().getElementsByTagName("uses-feature");
        boolean touchOptional = false;
        for (int i = 0; i < features.getLength(); i++) {
            Element feature = (Element) features.item(i);
            if ("android.hardware.touchscreen".equals(feature.getAttributeNS(ANDROID, "name"))) {
                touchOptional = "false".equals(feature.getAttributeNS(ANDROID, "required"));
            }
        }
        assertTrue(touchOptional);
        Element application = (Element) manifest().getElementsByTagName("application").item(0);
        assertEquals("@drawable/banner", application.getAttributeNS(ANDROID, "banner"));
        assertEquals("false", application.getAttributeNS(ANDROID, "allowBackup"));
    }

    @Test public void gradleKeepsThePackageNameAndTheSdkRange() throws Exception {
        String gradle = text(new File(module(), "build.gradle"));
        assertTrue(gradle.contains("applicationId 'com.cleanyourtv.launcher'"));
        assertTrue(gradle.contains("namespace 'com.cleanyourtv.launcher'"));
        Matcher min = Pattern.compile("minSdk (\\d+)").matcher(gradle);
        assertTrue(min.find());
        assertTrue("Android TV 9 is API 28", Integer.parseInt(min.group(1)) <= 28);
        // No libraries: the framework is enough, and the APK stays tiny.
        Matcher libraries = Pattern.compile("^\\s*(implementation|api)\\s", Pattern.MULTILINE).matcher(gradle);
        assertFalse("the launcher has no runtime dependency", libraries.find());
        assertFalse("pure Java: no native code", gradle.contains("ndk") || gradle.contains("externalNativeBuild"));
    }

    @Test public void englishAndFrenchHaveTheSameStrings() throws Exception {
        Set<String> english = stringNames(new File(module(), "src/main/res/values/strings.xml"));
        Set<String> french = stringNames(new File(module(), "src/main/res/values-fr/strings.xml"));
        assertNotNull(english);
        assertFalse(english.isEmpty());
        assertEquals(english, french);
    }

    private static Set<String> stringNames(File file) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        NodeList strings = doc.getElementsByTagName("string");
        Set<String> names = new HashSet<>();
        for (int i = 0; i < strings.getLength(); i++) {
            names.add(((Element) strings.item(i)).getAttribute("name"));
        }
        return names;
    }
}
