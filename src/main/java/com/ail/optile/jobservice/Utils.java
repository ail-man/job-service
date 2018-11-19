package com.ail.optile.jobservice;

public final class Utils {

    private Utils() {
    }

    public static String pingLocalhostCommand(int count) {
        String osName = System.getProperty("os.name");

        if (osName.startsWith("Windows")) {
            return "ping localhost -n " + count;
        } else if (osName.equals("Linux")) {
            return "ping localhost -c " + count;
        } else {
            return null;
        }
    }
}
