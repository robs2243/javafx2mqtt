package de.hssfds.rd.javafx2mqtt;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.eclipse.paho.client.mqttv3.*;

public class Main {

    @FXML
    private TextField inputField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button startButton;

    @FXML
    private Button terminateButton;

    @FXML
    private Button publishButton;

    // Reference to the Python process (the OPC UA/MQTT server)
    private Process pythonProcess;

    // MQTT settings
    private MqttClient mqttClient;
    private final String brokerUrl = "tcp://localhost:1883";
    private final String clientId = MqttClient.generateClientId();
    private final String topicOutput = "opc/output";  // Python publishes updated output here.
    private final String topicInput = "opc/input";    // GUI publishes input messages here.

    // MQTT Authentication Credentials
    private final String mqttUsername = "your_username";
    private final String mqttPassword = "your_password";

    @FXML
    public void initialize() {
        connectMqtt();
    }

    @FXML
    private void handleStart() {
        startPythonProcess();
    }

    @FXML
    private void handleTerminate() {
        terminateServers();
    }

    @FXML
    private void handlePublish() {
        publishMessage();
    }

    private void startPythonProcess() {
        try {
            if (pythonProcess == null || !pythonProcess.isAlive()) {
                // Adjust the command if needed (e.g., use "python3" on some systems)
                ProcessBuilder pb = new ProcessBuilder("python", "opc_mqtt_server.py");
                pb.redirectErrorStream(true);
                pythonProcess = pb.start();

                // Optionally print the Python process output in a background thread.
                new Thread(() -> {
                    try (var reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(pythonProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("Python: " + line);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void terminateServers() {
        // Terminate the Python process (OPC UA/MQTT server)
        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();
        }
        // Disconnect the MQTT client if connected.
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    private void publishMessage() {
        String message = inputField.getText();
        try {
            mqttClient.publish(topicInput, new MqttMessage(message.getBytes()));
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    private void connectMqtt() {
        try {
            mqttClient = new MqttClient(brokerUrl, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            // Set MQTT authentication credentials
            options.setUserName(mqttUsername);
            options.setPassword(mqttPassword.toCharArray());
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("MQTT connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    if (topic.equals(topicOutput)) {
                        String payload = new String(message.getPayload());
                        // Update the label on the JavaFX Application Thread.
                        Platform.runLater(() -> messageLabel.setText("Received: " + payload));
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used here.
                }
            });
            mqttClient.connect(options);
            mqttClient.subscribe(topicOutput);
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }
}
