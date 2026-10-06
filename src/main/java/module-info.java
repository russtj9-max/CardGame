module org.example.cardgame {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.cardgame to javafx.fxml;
    exports org.example.cardgame;
}