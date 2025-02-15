package de.hssfds.rd.javafx2mqtt;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.eclipse.paho.client.mqttv3.*;

public class MainController {

    @FXML
    private TextField tf_password;

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
    private final String topicOutput = "opc/output";  // Python publishes updated output here.
    private final String topicInput = "opc/input";    // GUI publishes input messages here.

    private final String myMQTTuser = "rd";
    private String myMQTTpass = "";
    private final String myMQTTbroker = "192.168.0.122";
    private final String myMQTTport = "1883";
    private final String myOPCUAip = "192.168.0.42";
    private final String myOPCUAport = "4840";

    private final String brokerUrl = "tcp://" + myMQTTbroker + ":" + myMQTTport;
    private final String clientId = MqttClient.generateClientId();

    @FXML
    public void initialize() {
        //connectMqtt();
    }

    @FXML
    private void handleStart() {
        myMQTTpass = tf_password.getText();
        startPythonProcess();
        if(mqttClient == null || !mqttClient.isConnected()) {
            connectMqtt();
        }
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
                String pythonPath = "C:\\proggen\\py\\opc2mqtt\\.venv\\Scripts\\python.exe";  // Windows
                String scriptPath = "C:\\proggen\\py\\opc2mqtt\\opc2mqtt.py";  // Windows

                ProcessBuilder pb = new ProcessBuilder(
                        pythonPath,
                        scriptPath,
                        "--username", myMQTTuser,
                        "--password", myMQTTpass,
                        "--mqtt-broker", myMQTTbroker,
                        "--mqtt-port", myMQTTport,
                        "--opcua-ip", myOPCUAip,
                        "--opcua-port", myOPCUAport
                );
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

        if(pythonProcess != null && mqttClient != null ) {

            // -101 to close opc ua server gracefully
            // py script will also terminate
            String message = "-101";
            inputField.setText(message);

            try {
                publishMessage();
                Thread.sleep(1000);  // Wait for the Python process to terminate.

                if (mqttClient != null && mqttClient.isConnected()) {
                    mqttClient.disconnect();
                    mqttClient.close();

                }

                pythonProcess = null;
                mqttClient = null;

                inputField.setText("0");
                //Platform.exit();

            } catch (InterruptedException | MqttException ex) {
                ex.printStackTrace();

            }
        }
    }

    private void publishMessage() {

        if(mqttClient != null && pythonProcess != null) {

            if (mqttClient.isConnected() && pythonProcess.isAlive()) {

                String message = inputField.getText();
                try {
                    mqttClient.publish(topicInput, new MqttMessage(message.getBytes()));
                } catch (MqttException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void connectMqtt() {
        try {
            mqttClient = new MqttClient(brokerUrl, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            // Set MQTT authentication credentials
            options.setUserName(myMQTTuser);
            options.setPassword(myMQTTpass.toCharArray());
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

    // Called by Main.java when the application is closing.
    public void cleanup() {
        terminateServers();
    }

}
