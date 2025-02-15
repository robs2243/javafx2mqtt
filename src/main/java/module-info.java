module de.hssfds.rd.javafx2mqtt {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.eclipse.paho.client.mqttv3;


    opens de.hssfds.rd.javafx2mqtt to javafx.fxml;
    exports de.hssfds.rd.javafx2mqtt;
}