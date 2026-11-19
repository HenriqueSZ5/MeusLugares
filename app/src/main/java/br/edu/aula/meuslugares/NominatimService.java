package br.edu.aula.meuslugares;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Geocodificação reversa no Nominatim, no mesmo molde do ViaCepService.
 * O servidor público pede no máximo 1 consulta por segundo e User-Agent próprio.
 */
public class NominatimService {

    private static final String URL_BASE = "https://nominatim.openstreetmap.org/reverse";
    private static final String USER_AGENT = "MeusLugares/1.0 (app academico Android; br.edu.aula.meuslugares)";
    private static final long INTERVALO_MINIMO_MS = 1100;

    private static long ultimaConsultaMs = 0;

    /** A API respondeu, mas sem endereço utilizável. */
    public static class EnderecoException extends Exception {
        public EnderecoException(String mensagem) {
            super(mensagem);
        }
    }

    /** Faz rede: não chamar na thread da tela. */
    public static String buscarEndereco(double latitude, double longitude)
            throws IOException, JSONException, EnderecoException {

        // Locale.US por causa da vírgula decimal do pt-BR
        URL url = new URL(String.format(Locale.US,
                "%s?format=jsonv2&lat=%.6f&lon=%.6f&zoom=18&addressdetails=1&accept-language=pt-BR",
                URL_BASE, latitude, longitude));

        aguardarIntervalo();
        HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
        try {
            conexao.setRequestMethod("GET");
            conexao.setRequestProperty("Accept", "application/json");
            conexao.setRequestProperty("User-Agent", USER_AGENT);
            conexao.setConnectTimeout(10000);
            conexao.setReadTimeout(10000);

            int status = conexao.getResponseCode();
            if (status == 429 || status == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new EnderecoException("Muitas consultas seguidas. Tente de novo em alguns segundos.");
            }
            if (status != HttpURLConnection.HTTP_OK) {
                throw new EnderecoException("Erro no servidor de endereços (código " + status + ")");
            }

            JSONObject json = new JSONObject(lerCorpo(conexao.getInputStream()));

            // Ex.: ponto no mar volta 200 com "error"
            if (json.has("error")) {
                throw new EnderecoException("Nenhum endereço encontrado para esta posição");
            }

            JSONObject endereco = json.optJSONObject("address");
            if (endereco == null) {
                String completo = json.optString("display_name");
                if (completo.isEmpty()) throw new EnderecoException("Nenhum endereço encontrado para esta posição");
                return completo;
            }
            return formatar(endereco);
        } finally {
            conexao.disconnect();
        }
    }

    // Segura o "Buscar endereço de novo" tocado várias vezes seguidas
    private static synchronized void aguardarIntervalo() throws InterruptedIOException {
        long espera = ultimaConsultaMs + INTERVALO_MINIMO_MS - System.currentTimeMillis();
        if (espera > 0) {
            try {
                Thread.sleep(espera);
            } catch (InterruptedException e) {
                // shutdownNow() do formulário
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Consulta cancelada");
            }
        }
        ultimaConsultaMs = System.currentTimeMillis();
    }

    /** "Rua, número, Bairro, Cidade - UF, CEP", só com o que vier preenchido. */
    private static String formatar(JSONObject a) {
        String rua = primeiro(a, "road", "pedestrian", "footway", "square");
        String numero = a.optString("house_number");
        String bairro = primeiro(a, "suburb", "neighbourhood", "quarter", "city_district");
        String cidade = primeiro(a, "city", "town", "village", "municipality");
        String uf = siglaUf(a);
        String cep = a.optString("postcode");

        List<String> partes = new ArrayList<>();
        if (!rua.isEmpty()) partes.add(numero.isEmpty() ? rua : rua + ", " + numero);
        if (!bairro.isEmpty()) partes.add(bairro);
        if (!cidade.isEmpty()) partes.add(uf.isEmpty() ? cidade : cidade + " - " + uf);
        if (!cep.isEmpty()) partes.add("CEP " + cep);
        return String.join(", ", partes);
    }

    // O nome do campo muda conforme o tipo de lugar (road, pedestrian...)
    private static String primeiro(JSONObject json, String... chaves) {
        for (String chave : chaves) {
            String valor = json.optString(chave);
            if (!valor.isEmpty()) return valor;
        }
        return "";
    }

    // "BR-BA" vira "BA"
    private static String siglaUf(JSONObject a) {
        String codigo = a.optString("ISO3166-2-lvl4");
        if (codigo.startsWith("BR-")) return codigo.substring(3);
        return a.optString("state");
    }

    private static String lerCorpo(InputStream entrada) throws IOException {
        StringBuilder texto = new StringBuilder();
        try (BufferedReader leitor = new BufferedReader(
                new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                texto.append(linha);
            }
        }
        return texto.toString();
    }
}
