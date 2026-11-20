package com.library.util;

/**
 * Utility for rendering formatted tables, banners, and divider lines in the console.
 */
public class ConsoleUI {

    public static void printHeader(String title) {
        System.out.println();
        System.out.println("==========================================================================================");
        System.out.println("   " + title.toUpperCase());
        System.out.println("==========================================================================================");
    }

    public static void printSubHeader(String subTitle) {
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println(" > " + subTitle);
        System.out.println("------------------------------------------------------------------------------------------");
    }

    public static void printDivider() {
        System.out.println("------------------------------------------------------------------------------------------");
    }

    public static void printSuccess(String message) {
        System.out.println("[SUCCESS] " + message);
    }

    public static void printError(String message) {
        System.out.println("[ERROR] " + message);
    }

    public static void printWarning(String message) {
        System.out.println("[WARNING] " + message);
    }

    public static void printInfo(String message) {
        System.out.println("[INFO] " + message);
    }
}
