module com.dhasboard.chat {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;

    opens com.dhasboard.chat to javafx.fxml;
    exports com.dhasboard.chat;
}