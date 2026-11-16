package br.edu.aula.meuslugares;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conversa com a API Nominatim (OpenStreetMap) para transformar uma coordenada
 * em endereço ("geocodificação reversa"). Mesmo padrão do ViaCepService da aula de APIs.
 *
 * Regras de uso do servidor público: no máximo 1 requisição por segundo e
 * um User-Agent que identifique o app.
 */
public class NominatimService {

    private static final String URL_BASE = "https://nominatim.openstreetmap.org/reverse";
    private static final String USER_AGENT = "MeusLugares/1.0 (app academico Android; br.edu.aula.meuslugares)";

    /** Erros "de negócio": a API respondeu, mas não há endereço para devolver. */
    public static class EnderecoException extends Exception {
        public EnderecoException(String mensagem) {
            super(mensagem);
        }
    }

    /** Faz o GET e devolve o endereço já formatado em uma linha. Rodar SEMPRE em thread de fundo. */
    public static String buscarEndereco(double latitude, double longitude)
            throws IOException, JSONException, EnderecoException {

        // Locale.US garante ponto decimal (em pt-BR seria vírgula e a URL ficaria errada)
        URL url = new URL(String.format(Locale.US,
                "%s?format=jsonv2&lat=%.6f&lon=%.6f&zoom=18&addressdetails=1&accept-language=pt-BR",
                URL_BASE, latitude, longitude));

        HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
        try {
            conexao.setRequestMethod("GET");
            conexao.setRequestProperty("Accept", "application/json");
            conexao.setRequestProperty("User-Agent", USER_AGENT); // exigido pelo Nominatim
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

            // Coordenada no meio do mar, por exemplo: status 200 com o campo "error"
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

    /** Monta "Rua, número - Bairro, Cidade - UF, CEP" com os campos que vierem. */
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

    /** Devolve o primeiro campo não vazio da lista (o nome do campo varia conforme o lugar). */
    private static String primeiro(JSONObject json, String... chaves) {
        for (String chave : chaves) {
            String valor = json.optString(chave);
            if (!valor.isEmpty()) return valor;
        }
        return "";
    }

    /** O Nominatim manda o estado por extenso ("Bahia") e o código "BR-BA"; usamos a sigla. */
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
