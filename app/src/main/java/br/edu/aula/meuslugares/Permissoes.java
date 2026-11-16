package br.edu.aula.meuslugares;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

/**
 * Diálogos usados no fluxo de permissões (aula de Permissões), compartilhados
 * entre as telas que pedem localização e câmera.
 */
public final class Permissoes {

    private Permissoes() { }

    /** Justificativa mostrada quando o usuário já negou uma vez. Se ele aceitar, pedimos de novo. */
    public static void mostrarExplicacao(Activity activity, String titulo, String mensagem,
                                         Runnable aoContinuar) {
        new AlertDialog.Builder(activity)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton("Continuar", (dialogo, botao) -> aoContinuar.run())
                .setNegativeButton("Agora não", null)
                .show();
    }

    /** Permissão negada de vez: só dá para liberar nas Configurações do app. */
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
