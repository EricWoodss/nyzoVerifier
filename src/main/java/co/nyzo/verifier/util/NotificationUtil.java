package co.nyzo.verifier.util;

import co.nyzo.verifier.Verifier;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationUtil {


    private static int numberOfNotificationsSent = 0;
    private static final int maximumNotifications = 10;
    private static final Set<Integer> sendOnceNotifications = new HashSet<>();
    private static long notificationLimitReachedTimestamp = Long.MAX_VALUE;

    private static final String endpoint = loadEndpointFromFile();

    public static void send(String message) {

        // Reset the limit 10 minutes after hitting it.
        if (numberOfNotificationsSent >= maximumNotifications &&
                notificationLimitReachedTimestamp < System.currentTimeMillis() - 1000L * 60L * 10L) {
            numberOfNotificationsSent = 0;
            notificationLimitReachedTimestamp = Long.MAX_VALUE;
        }

        if (endpoint != null && numberOfNotificationsSent < maximumNotifications) {

            numberOfNotificationsSent++;
            if (numberOfNotificationsSent >= maximumNotifications) {
                message = "*Message limit reached on " + Verifier.getNickname() + "*";
                notificationLimitReachedTimestamp = System.currentTimeMillis();
            }

            try {
                String jsonString = "{\"text\":\"" + message + "\"}";

                URL url = new URL(endpoint);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestMethod("POST");

                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
                outputStreamWriter.write(jsonString);
                outputStreamWriter.flush();

                StringBuilder result = new StringBuilder();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                            "UTF-8"));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line + "\n");
                    }
                    reader.close();

                    System.out.println(result.toString());
                } else {
                    System.out.println(connection.getResponseMessage());
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }

    public static void sendOnce(String message) {

        int hashCode = message.hashCode();
        if (!sendOnceNotifications.contains(hashCode)) {
            sendOnceNotifications.add(hashCode);
            send(message);
        }
    }

    private static String loadEndpointFromFile() {

        String endpoint = null;
        try {
            List<String> fileContents = Files.readAllLines(Paths.get("/var/lib/nyzo/notification_config"));
            if (fileContents.size() > 0) {
                endpoint = fileContents.get(0).trim();
                if (endpoint.isEmpty()) {
                    endpoint = null;
                }
            }
        } catch (Exception ignored) { }

        return endpoint;
    }
}
