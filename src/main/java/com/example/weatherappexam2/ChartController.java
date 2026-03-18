package com.example.weatherappexam2;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.json.JSONArray;

public class ChartController {

    @FXML private Label titleLabel;
    @FXML private LineChart<String, Number> lineChart;

    public void setupChart(String title, String unit, JSONArray times, JSONArray values) {
        titleLabel.setText(title + " (" + unit + ")");
        lineChart.setTitle(title);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(title);

        for (int i = 0; i < times.length(); i++) {
            String time = times.getString(i).replace("T", " ");
            Number val = values.getNumber(i);
            series.getData().add(new XYChart.Data<>(time, val));
        }

        lineChart.getData().add(series);
    }
}