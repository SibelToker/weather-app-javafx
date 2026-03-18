package com.example.weatherappexam2;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class HelloController {

    @FXML private TextField daysField, latField, lonField, cityField;
    @FXML private RadioButton radioLatLon, radioName;
    @FXML private CheckBox checkTemp, checkRain, checkWind, checkPressure;
    @FXML private Label infoLabel;

    private final WeatherService service = new WeatherService();

    @FXML
    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        radioLatLon.setToggleGroup(group);
        radioName.setToggleGroup(group);

        radioLatLon.setOnAction(e -> {
            latField.setDisable(false); lonField.setDisable(false); cityField.setDisable(true);
        });
        radioName.setOnAction(e -> {
            latField.setDisable(true); lonField.setDisable(true); cityField.setDisable(false);
        });
    }

    @FXML
    protected void onFetchClick() {
        try {
            double lat = 0, lon = 0;
            String locInfo = "";

            // 1. LOKASYON BULMA VE LİSTELEME
            if (radioName.isSelected()) {
                String city = cityField.getText().trim();
                if(city.isEmpty()) { infoLabel.setText("Please enter city name."); return; }

                // Servisten tüm veriyi (listeyi) çekiyoruz
                JSONObject fullJson = service.getCoordinates(city);

                if (fullJson == null) { infoLabel.setText("City not found!"); return; }

                JSONArray results = fullJson.getJSONArray("results");

                // --- HOCANIN İSTEDİĞİ LİSTELEME (POP-UP) ---
                StringBuilder listMsg = new StringBuilder("Found " + results.length() + " locations:\n\n");

                for (int i = 0; i < results.length(); i++) {
                    JSONObject obj = results.getJSONObject(i);
                    String rName = obj.getString("name");
                    String rCountry = obj.has("country") ? obj.getString("country") : "-";
                    String rAdmin = obj.has("admin1") ? obj.getString("admin1") : ""; // Eyalet/Bölge
                    double rLat = obj.getDouble("latitude");
                    double rLon = obj.getDouble("longitude");

                    // Listeye ekle: "1. Berlin, Germany (State of Berlin) [52.52, 13.41]"
                    listMsg.append(String.format("%d. %s, %s %s [%.2f, %.2f]\n",
                            (i+1), rName, rCountry, rAdmin, rLat, rLon));
                }

                // Pop-up Göster
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Search Results");
                alert.setHeaderText("Multiple results found for: " + city);
                alert.setContentText(listMsg.toString());
                alert.getDialogPane().setMinWidth(400); // Pencere çok dar olmasın
                alert.showAndWait();
                // ------------------------------------------

                // SINAV KURALI: İlk sonucu kullanmaya devam ediyoruz
                JSONObject firstResult = results.getJSONObject(0);
                lat = firstResult.getDouble("latitude");
                lon = firstResult.getDouble("longitude");
                String name = firstResult.getString("name");
                String country = firstResult.has("country") ? firstResult.getString("country") : "Unknown";

                locInfo = name + ", " + country;
                infoLabel.setText(String.format("Used 1st result: %s\n(See popup for others)", locInfo));

            } else {
                lat = Double.parseDouble(latField.getText());
                lon = Double.parseDouble(lonField.getText());
                locInfo = "Lat: " + lat + ", Lon: " + lon;
                infoLabel.setText("Used: " + locInfo);
            }

            // 2. PARAMETRELERİ HAZIRLA
            List<String> params = new ArrayList<>();
            if (checkTemp.isSelected()) params.add("temperature_2m");
            if (checkRain.isSelected()) params.add("precipitation");
            if (checkWind.isSelected()) params.add("wind_speed_10m");
            if (checkPressure.isSelected()) params.add("surface_pressure");

            if (params.isEmpty()) { infoLabel.setText("Select variables!"); return; }

            // 3. VERİYİ ÇEK VE GÖSTER
            String response = service.getWeatherData(lat, lon, Integer.parseInt(daysField.getText()), String.join(",", params));
            parseAndShow(response, params, locInfo);

        } catch (Exception e) {
            infoLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void parseAndShow(String jsonStr, List<String> params, String loc) {
        JSONObject json = new JSONObject(jsonStr);

        if (!json.has("hourly")) {
            infoLabel.setText("Error: No hourly data found.");
            return;
        }

        JSONObject hourly = json.getJSONObject("hourly");
        JSONObject units = json.getJSONObject("hourly_units");
        JSONArray times = hourly.getJSONArray("time");

        for (String p : params) {
            try {
                if (!hourly.has(p)) continue;

                FXMLLoader loader = new FXMLLoader(getClass().getResource("chart-view.fxml"));
                Scene scene = new Scene(loader.load());
                ChartController ctrl = loader.getController();

                String unit = units.has(p) ? units.getString(p) : "";
                ctrl.setupChart(p + " - " + loc, unit, times, hourly.getJSONArray(p));

                Stage stage = new Stage();
                stage.setTitle(p + " Chart");
                stage.setScene(scene);
                stage.show();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}