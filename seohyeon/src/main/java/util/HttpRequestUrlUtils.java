package util;

import com.google.common.base.Strings;

public class HttpRequestUrlUtils {

    public static String parseRequestMethod(String line) {
        if (!Strings.isNullOrEmpty(line)) {
            String[] firstLine = line.split(" ");
            if (!firstLine[0].isEmpty()) return firstLine[0];
        }
        return "";
    }

    public static String parseRequestUrl(String line) {
        if (!Strings.isNullOrEmpty(line)) {
            String[] firstLine = line.split(" ");
            if (!firstLine[1].isEmpty()) return firstLine[1];
        }
        return "";
    }

}
