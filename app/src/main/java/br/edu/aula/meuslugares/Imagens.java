package br.edu.aula.meuslugares;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/** Leitura de fotos em tamanho reduzido (mesma técnica do MidiaAdapter da aula de SQLite). */
public final class Imagens {

    private Imagens() { }

    /**
     * Lê a foto reduzida. Uma foto da câmera pode ter 12 MP ou mais;
     * carregá-la inteira causaria OutOfMemoryError.
     */
    public static Bitmap carregarReduzida(String caminho, int tamanhoAlvo) {
        if (caminho == null) return null;

        // 1ª leitura: só descobre largura e altura, sem carregar os pixels
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(caminho, opcoes);
        if (opcoes.outWidth <= 0 || opcoes.outHeight <= 0) return null;

        // Calcula quanto reduzir (potência de 2)
        int fator = 1;
        while (opcoes.outWidth / (fator * 2) >= tamanhoAlvo
                && opcoes.outHeight / (fator * 2) >= tamanhoAlvo) {
            fator *= 2;
        }

        // 2ª leitura: carrega a imagem já reduzida
        opcoes.inJustDecodeBounds = false;
        opcoes.inSampleSize = fator;
        return BitmapFactory.decodeFile(caminho, opcoes);
    }
}
