package com.example.semafx;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class SemaforoController {
    @FXML
    private GridPane board;

    @FXML
    private Label indicadorVezLabel;
    @FXML
    private Label pecasVerdeLabel;
    @FXML
    private Label pecasAmarelaLabel;

    @FXML
    private Label pecasVermelhasLabel;

    private final int ROWS = 3;
    private final int COLS = 4;
    private final int TAMANHO_PECAS = 50;

    private enum Piece {VERDE, AMARELO, VERMELHO, VAZIO, REINICIAR, VITORIA}
    private Piece[][] grelha = new Piece[ROWS][COLS];

    private int pecasVerde = 8;
    private int pecasAmarela = 8;
    private int pecasVermelhas = 8;
    private boolean isVezJogador = false;

    private SemaforoClient client;

    public void setClient(SemaforoClient client) {
        this.client = client;
    }

    //Atualiza o turno do jogador
    public void setVezJogador(boolean isVezJogador) {
        this.isVezJogador = isVezJogador;
        AtualizarVez();
    }

    //Inicializa o tabuleiro
    public void initialize() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                grelha[i][j] = Piece.VAZIO;
                Rectangle square = new Rectangle(TAMANHO_PECAS, TAMANHO_PECAS);
                square.setFill(Color.WHITE);
                square.setStroke(Color.BLACK);
                board.add(square, j, i);

                // Add click event to each cell
                final int row = i;
                final int col = j;
                square.setOnMouseClicked(event -> atuarCliqueTabuleiro(row, col));
            }
        }
        atualizarContagemPecas();
        AtualizarVez();
    }

    //Reage as jogadas feitas no tabuleiro
    private void atuarCliqueTabuleiro(int row, int col) {
        if (!isVezJogador) {
            mostrarAlerta("Espere a sua vez!", "Nada de batotas!");
            return;
        }
        Piece currentPiece = grelha[row][col];
        Piece nextPiece = Piece.VAZIO;
        switch (currentPiece) {
            case VAZIO:
                if (pecasVerde > 0) {
                    nextPiece = Piece.VERDE;
                } else {
                    mostrarAlerta("Acabaram as peças verdes!", "Oops!");
                    return;
                }
                break;
            case VERDE:
                if (pecasAmarela > 0) {
                    nextPiece = Piece.AMARELO;
                } else {
                    mostrarAlerta("Acabaram as peças amarelas!","Oops!");
                    return;
                }
                break;
            case AMARELO:
                if (pecasVermelhas > 0) {
                    nextPiece = Piece.VERMELHO;
                } else {
                    mostrarAlerta("Acabaram as peças vermelhas!","Oops!");
                    return;
                }
                break;
            case VERMELHO:
                return; // Red pieces cannot be replaced
        }
        client.enviarJogada(row, col, nextPiece.name());
        isVezJogador = false;
        AtualizarVez();
    }

    //Atualiza o tabuleiro local perante a jogada feito pelo adversario
    public void atualizarTabuleiroLocal(int row, int col, String piece) {
        if (piece.equals("REINICIAR")) {
            reiniciarJogo();
            return;
        }
        Piece p = Piece.valueOf(piece);
        switch (p) {
            case VERDE:
                grelha[row][col] = Piece.VERDE;
                atualizarPeca(row, col, new Circle(TAMANHO_PECAS / 2, Color.GREEN));
                pecasVerde--;
                break;
            case AMARELO:
                grelha[row][col] = Piece.AMARELO;
                atualizarPeca(row, col, criarTriangulo(TAMANHO_PECAS, Color.YELLOW));
                pecasVerde++;
                pecasAmarela--;
                break;
            case VERMELHO:
                grelha[row][col] = Piece.VERMELHO;
                atualizarPeca(row, col, new Rectangle(TAMANHO_PECAS, TAMANHO_PECAS, Color.RED));
                pecasAmarela++;
                pecasVermelhas--;
                break;
        }
        atualizarContagemPecas();
        if (validarVitoria(row, col, p)) {
            client.enviarJogada(row, col, "VITORIA");
            mostrarAlerta("Ganhaste!", "Parabéns!");
            client.enviarJogada(-1, -1, "REINICIAR");
        }
    }

    //Passa a jogada feita localmente ao servidor para atualizar o adversario
    public void aturaJogadaServidor(int row, int col, String piece) {
        if (piece.equals("REINICIAR")) {
            reiniciarJogo();
            isVezJogador = true; // Player who did not win starts
            AtualizarVez();
            return;
        }
        if (piece.equals("VITORIA")) {
            mostrarAlerta("Ganhaste!","Parabéns!");
            client.enviarJogada(-1, -1, "REINICIAR"); // Notify server to reset game
            reiniciarJogo();
            return;
        }
        Piece p = Piece.valueOf(piece);
        switch (p) {
            case VERDE:
                grelha[row][col] = Piece.VERDE;
                atualizarPeca(row, col, new Circle(TAMANHO_PECAS / 2, Color.GREEN));
                pecasVerde--;
                break;
            case AMARELO:
                grelha[row][col] = Piece.AMARELO;
                atualizarPeca(row, col, criarTriangulo(TAMANHO_PECAS, Color.YELLOW));
                pecasVerde++;
                pecasAmarela--;
                break;
            case VERMELHO:
                grelha[row][col] = Piece.VERMELHO;
                atualizarPeca(row, col, new Rectangle(TAMANHO_PECAS, TAMANHO_PECAS, Color.RED));
                pecasAmarela++;
                pecasVermelhas--;
                break;
        }
        atualizarContagemPecas();
        if (validarVitoria(row, col, p)) {
            mostrarAlerta("Perdeu!","Boa sorte na proxima vez!");
            reiniciarJogo();
        }
    }

    //Cria a peça amarela triangular
    private Polygon criarTriangulo(double size, Color color) {
        Polygon triangle = new Polygon();
        triangle.getPoints().addAll(
                size / 2, 0.0,
                0.0, size,
                size, size
        );
        triangle.setFill(color);
        return triangle;
    }

    //Troca as peças nas jogadas (verde por amarelo, amarelo por vermelho perante o que aparecer
    private void atualizarPeca(int row, int col, Shape newPiece) {
        Shape oldPiece = getFormaNoTabuleiro(row, col);
        if (oldPiece != null) {
            board.getChildren().remove(oldPiece);
        }
        board.add(newPiece, col, row);
        newPiece.setOnMouseClicked(event -> atuarCliqueTabuleiro(row, col));
    }

    //Avalia a peça clicada no tabuleiro
    private Shape getFormaNoTabuleiro(int row, int col) {
        for (javafx.scene.Node node : board.getChildren()) {
            if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == col) {
                if (node instanceof Shape) {
                    return (Shape) node;
                }
            }
        }
        return null;
    }

    //Atualiza a contagem de cada peça
    private void atualizarContagemPecas() {
        pecasVerdeLabel.setText("Peças Verdes: " + pecasVerde);
        pecasAmarelaLabel.setText("Peças Amarelas: " + pecasAmarela);
        pecasVermelhasLabel.setText("Peças Vermelhas: " + pecasVermelhas);
    }

    //Atualiza o turno após a jogada de cada jogador
    private void AtualizarVez() {
        indicadorVezLabel.setText(isVezJogador ? "Tua vez" : "Vez do adversário");
    }

    //Verifica se alguem ganhou
    private boolean validarVitoria(int row, int col, Piece piece) {
        return validarDireccao(row, col, piece, 1, 0) || // Horizontal
                validarDireccao(row, col, piece, 0, 1) || // Vertical
                validarDireccao(row, col, piece, 1, 1) || // Diagonal \
                validarDireccao(row, col, piece, 1, -1);  // Diagonal /
    }

    //Valida as direçao para procurar vitorias (vericalmente, horizontalmente e nas diagonais)
    private boolean validarDireccao(int row, int col, Piece piece, int rowDir, int colDir) {
        int count = 0;
        for (int i = -2; i <= 2; i++) {
            int r = row + i * rowDir;
            int c = col + i * colDir;
            if (r >= 0 && r < ROWS && c >= 0 && c < COLS && grelha[r][c] == piece) {
                count++;
                if (count == 3) {
                    return true;
                }
            } else {
                count = 0;
            }
        }
        return false;
    }

    //Mostra alertas
    public void mostrarAlerta(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //Reinicia o jogo no fim do mesmo.
    public void reiniciarJogo() {
        board.getChildren().clear();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                grelha[i][j] = Piece.VAZIO;
                Rectangle square = new Rectangle(TAMANHO_PECAS, TAMANHO_PECAS);
                square.setFill(Color.WHITE);
                square.setStroke(Color.BLACK);
                board.add(square, j, i);

                final int row = i;
                final int col = j;
                square.setOnMouseClicked(event -> atuarCliqueTabuleiro(row, col));
            }
        }
        pecasVerde = 8;
        pecasAmarela = 8;
        pecasVermelhas = 8;
        atualizarContagemPecas();
        AtualizarVez();
    }
}
