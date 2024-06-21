module com.example.semafx {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.semafx to javafx.fxml;
    exports com.example.semafx;
}