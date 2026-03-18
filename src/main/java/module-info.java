module com.example.weatherappexam2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires org.json;

    opens com.example.weatherappexam2 to javafx.fxml;
    exports com.example.weatherappexam2;
}