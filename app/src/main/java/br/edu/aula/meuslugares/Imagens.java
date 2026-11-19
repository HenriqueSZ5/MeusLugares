package br.edu.aula.meuslugares;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/** Mesma técnica do MidiaAdapter da aula de SQLite. */
public final class Imagens {

    private Imagens() { }

    // Foto de câmera tem 12 MP ou mais; carregar inteira estoura a memória
    public static Bitmap carregarReduzida(String caminho, int tamanhoAlvo) {
        if (caminho == null) return null;

        // Primeiro só lê o tamanho, sem os pixels
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(caminho, opcoes);
        if (opcoes.outWidth <= 0 || opcoes.outHeight <= 0) return null;

        int fator = 1;
        while (opcoes.outWidth / (fator * 2) >= tamanhoAlvo
                && opcoes.outHeight / (fator * 2) >= tamanhoAlvo) {
            fator *= 2;
        }

        opcoes.inJustDecodeBounds = false;
        opcoes.inSampleSize = fator;
        return BitmapFactory.decodeFile(caminho, opcoes);
    }
}
