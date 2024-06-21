package com.example.semafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SemaforoClient extends Application {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private SemaforoController controller;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SemaforoClient.class.getResource("semaforo-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 400);
        stage.setTitle("Semáforo");
        stage.setScene(scene);
        stage.show();

        controller = fxmlLoader.getController();
        conectarServidor();
        controller.setClient(this);
    }

    //Funçao que conecta se ao servidor
    private void conectarServidor() throws IOException {
        socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        new Thread(() -> {
            try {
                while (true) {
                    String response = in.readLine();
                    if (response == null) {
                        break;
                    }
                    atuarRespostaServidor(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //Funçao que manda a jogada ao servidor
    public void enviarJogada(int row, int col, String piece) {
        Platform.runLater(() -> controller.atualizarTabuleiroLocal(row, col, piece));
        out.println(row + "," + col + "," + piece);
    }

    //Atua perante a emissao de uma nova jogada do adversario
    private void atuarRespostaServidor(String response) {
        Platform.runLater(() -> {
            String[] parts = response.split(",");
            switch (parts[0]) {
                case "TUA_VEZ":
                    controller.setVezJogador(true);
                    break;
                case "ESPERAR_VEZ":
                    controller.setVezJogador(false);
                    break;
                case "OPONENTE_JOGOU":
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    String piece = parts[3];
                    controller.aturaJogadaServidor(row, col, piece);
                    break;
                case "VITORIA":
                    controller.mostrarAlerta("You win!", "Congratulations!");
                    enviarJogada(-1, -1, "RESET");
                    break;
                case "DERROTA":
                    controller.mostrarAlerta("You lose!","Better luck next time!");
                    enviarJogada(-1, -1, "RESET");
                    break;
                case "REINICIAR":
                    controller.reiniciarJogo();
                    controller.setVezJogador(false);
                    break;
            }
        });
    }
}
