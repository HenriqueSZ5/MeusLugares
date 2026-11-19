package br.edu.aula.meuslugares;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

/** Diálogos de permissão usados pela tela do mapa e pela do formulário. */
public final class Permissoes {

    private Permissoes() { }

    /** Para quando o usuário já negou uma vez. */
    public static void mostrarExplicacao(Activity activity, String titulo, String mensagem,
                                         Runnable aoContinuar) {
        new AlertDialog.Builder(activity)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton("Continuar", (dialogo, botao) -> aoContinuar.run())
                .setNegativeButton("Agora não", null)
                .show();
    }

    /** Negada de vez: só pelas Configurações. */
    public static void mostrarDialogoConfiguracoes(Activity activity, String mensagem) {
        new AlertDialog.Builder(activity)
                .setTitle("Permissão negada")
                .setMessage(mensagem)
                .setPositiveButton("Abrir Configurações", (dialogo, botao) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
                    activity.startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
