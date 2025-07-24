package common;

import java.io.*;
import java.time.LocalDateTime;

public class LoggerUtil {
    private static final String LOG_FILE = "auth_log.txt";

    public static void log(String entry) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write(LocalDateTime.now() + ": " + entry + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
