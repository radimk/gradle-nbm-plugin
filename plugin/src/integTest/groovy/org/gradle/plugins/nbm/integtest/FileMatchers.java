package org.gradle.plugins.nbm.integtest;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.File;

public class FileMatchers {

    public static Matcher<File> exists() {
        return new TypeSafeMatcher<File>() {
            File fileTested;

            public boolean matchesSafely(File item) {
                fileTested = item;
                return item.exists();
            }

            public void describeTo(Description description) {
                description.appendText(" that file ");
                description.appendValue(fileTested);
                description.appendText(" exists");
            }
        };
    }

    public static Matcher<File> isFile() {
        return new TypeSafeMatcher<File>() {
            File fileTested;

            public boolean matchesSafely(File item) {
                fileTested = item;
                return item.isFile();
            }

            public void describeTo(Description description) {
                description.appendText(" that ");
                description.appendValue(fileTested);
                description.appendText("is a file");
            }
        };
    }
}
