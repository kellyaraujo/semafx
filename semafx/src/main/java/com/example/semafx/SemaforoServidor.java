package com.example.semafx;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class SemaforoServidor {
    private static final int PORT = 12345;
    private static List<SemaforoClientHandler> clients = new ArrayList<>();
    private static int indiceJogadorAtual = 0;

    public static void main(String[] args) {
        System.out.println("Servidor iniciou...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                SemaforoClientHandler clientHandler = new SemaforoClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
                if (clients.size() == 2) {
                    alertarVez();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Alerta a vez de cada jogador
    private static void alertarVez() {
        for (int i = 0; i < clients.size(); i++) {
            if (i == indiceJogadorAtual) {
                clients.get(i).enviarMensagem("TUA_VEZ");
            } else {
                clients.get(i).enviarMensagem("ESPERAR_VEZ");
            }
        }
    }

    //Emite a jogada feita a todos jogadores
    private static synchronized void fazerJogada(int row, int col, String piece) {
        String movemensagem = "OPONENTE_JOGOU," + row + "," + col + "," + piece;
        for (int i = 0; i < clients.size(); i++) {
            if (i != indiceJogadorAtual) {
                clients.get(i).enviarMensagem(movemensagem);
            }
        }
        indiceJogadorAtual = (indiceJogadorAtual + 1) % clients.size();
        alertarVez();
    }

    //Responsavel entre criar a ponte entre todos os jogadores
    private static class SemaforoClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;

        public SemaforoClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String mensagem;
                while ((mensagem = in.readLine()) != null) {
                    String[] parts = mensagem.split(",");
                    int row = Integer.parseInt(parts[0]);
                    int col = Integer.parseInt(parts[1]);
                    String piece = parts[2];
                    fazerJogada(row, col, piece);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Envia mensagem aos jogadores
        public void enviarMensagem(String mensagem) {
            out.println(mensagem);
        }
    }
}
