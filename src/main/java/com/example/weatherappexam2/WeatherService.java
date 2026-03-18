package com.example.weatherappexam2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class WeatherService {

    private final HttpClient client = HttpClient.newHttpClient();

    // 1. Koordinatları Bul (Tüm sonuçları döndürür)
    public JSONObject getCoordinates(String cityName) throws Exception {
        // count=10 yaptık ki farklı olasılıkları görebilelim
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + cityName.replace(" ", "%20") + "&count=10&language=en&format=json";

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject json = new JSONObject(response.body());

        // Eğer "results" yoksa null dönüyoruz
        if (!json.has("results")) return null;

        return json;
    }

    // 2. Hava Durumu Verisi (Cache mekanizması + Locale Fix)
    public String getWeatherData(double lat, double lon, int days, String params) throws Exception {
        // Dosya ismi parametrelerle benzersiz yapıldı
        String fileName = String.format(Locale.US, "cache_%.2f_%.2f_%d_%s.json", lat, lon, days, params.hashCode());
        File cacheFile = new File(fileName);

        // 3 Saat (180 dk) Kuralı
        if (cacheFile.exists()) {
            long diffInMillis = System.currentTimeMillis() - cacheFile.lastModified();
            long diffInMinutes = diffInMillis / (60 * 1000);

            if (diffInMinutes < 180) {
                System.out.println("Reading from CACHE: " + fileName);
                return Files.readString(Path.of(fileName));
            }
        }

        System.out.println("Fetching from INTERNET...");

        // Locale.US ile nokta formatında (52.52) gönderiyoruz
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&hourly=%s&forecast_days=%d",
                lat, lon, params, days
        );

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.body().contains("\"error\":true")) {
            throw new Exception("API Error: Coordinates might be wrong format.");
        }

        try (FileWriter writer = new FileWriter(cacheFile)) {
            writer.write(response.body());
        }

        return response.body();
    }
}