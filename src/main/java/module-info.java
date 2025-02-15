module de.hssfds.rd.javafx2mqtt {
    requires javafx.controls;
    requires javafx.fxml;


    opens de.hssfds.rd.javafx2mqtt to javafx.fxml;
    exports de.hssfds.rd.javafx2mqtt;
}