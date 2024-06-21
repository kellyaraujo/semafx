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
        initializeBoard();
        atualizarContagemPecas();
        AtualizarVez();
    }

    // Reinitialize the board
    private void initializeBoard() {
        board.getChildren().clear(); // Clear the board
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
                    mostrarAlerta("Acabaram as peças amarelas!", "Oops!");
                    return;
                }
                break;
            case AMARELO:
                if (pecasVermelhas > 0) {
                    nextPiece = Piece.VERMELHO;
                } else {
                    mostrarAlerta("Acabaram as peças vermelhas!", "Oops!");
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
            mostrarAlerta("Ganhaste!", "Parabéns!");
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
            mostrarAlerta("Perdeu!", "Boa sorte na proxima vez!");
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

    //Avalia a peça clicada e que esta no tabuleiro, retorna null se vazio
    private Shape getFormaNoTabuleiro(int row, int col) {
        for (var node : board.getChildren()) {
            if (board.getRowIndex(node) == row && board.getColumnIndex(node) == col && node instanceof Shape) {
                return (Shape) node;
            }
        }
        return null;
    }

    //Mostra um alerta para o jogador
    public void mostrarAlerta(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    //Atualiza a contagem de peças
    private void atualizarContagemPecas() {
        pecasVerdeLabel.setText("Peças Verdes: " + pecasVerde);
        pecasAmarelaLabel.setText("Peças Amarelas: " + pecasAmarela);
        pecasVermelhasLabel.setText("Peças Vermelhas: " + pecasVermelhas);
    }

    //Indica a vez do jogador ou adversario
    private void AtualizarVez() {
        indicadorVezLabel.setText(isVezJogador ? "A sua vez" : "Vez do adversário");
    }

    //Verifica o estado do tabuleiro para ver se há vitória
    private boolean validarVitoria(int row, int col, Piece piece) {
        return checkRow(row, piece) || checkColumn(col, piece) || checkDiagonals(piece);
    }

    private boolean checkRow(int row, Piece piece) {
        for (int col = 0; col < COLS; col++) {
            if (grelha[row][col] != piece) {
                return false;
            }
        }
        return true;
    }

    private boolean checkColumn(int col, Piece piece) {
        for (int row = 0; row < ROWS; row++) {
            if (grelha[row][col] != piece) {
                return false;
            }
        }
        return true;
    }

    private boolean checkDiagonals(Piece piece) {
        boolean leftToRight = true, rightToLeft = true;
        for (int i = 0; i < Math.min(ROWS, COLS); i++) {
            if (grelha[i][i] != piece) {
                leftToRight = false;
            }
            if (grelha[i][COLS - i - 1] != piece) {
                rightToLeft = false;
            }
        }
        return leftToRight || rightToLeft;
    }

    //Reinicia o jogo
    public void reiniciarJogo() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                grelha[i][j] = Piece.VAZIO;
                Shape piece = getFormaNoTabuleiro(i, j);
                if (piece != null) {
                    board.getChildren().remove(piece);
                }
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
    }

    // Mostrar as regras do jogo
    @FXML
    private void mostrarRegras() {
        String regras = "Regras do Jogo Semáforo:\n\n" +
                "1. O tabuleiro tem 3 linhas e 4 colunas.\n" +
                "2. Cada jogador começa com 8 peças verdes, 8 amarelas e 8 vermelhas.\n" +
                "3. Clique em um espaço vazio para colocar uma peça verde.\n" +
                "4. Clique em uma peça verde para substituí-la por uma peça amarela.\n" +
                "5. Clique em uma peça amarela para substituí-la por uma peça vermelha.\n" +
                "6. Peças vermelhas não podem ser substituídas.\n" +
                "7. Vence o jogador que alinhar 4 peças da mesma cor em uma linha, coluna ou diagonal.";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Regras do Jogo");
        alert.setHeaderText(null);
        alert.setContentText(regras);
        alert.showAndWait();
    }
}
