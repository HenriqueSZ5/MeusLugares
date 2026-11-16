package br.edu.aula.meuslugares;

/**
 * Classe modelo que representa UMA linha da tabela "lugares".
 * Cada atributo corresponde a uma coluna do banco.
 */
public class Lugar {

    private long id;              // chave primária (gerada pelo SQLite)
    private String nome;          // nome digitado pelo usuário
    private final double latitude;
    private final double longitude;
    private final String endereco;    // vindo da API Nominatim (pode ser null)
    private final String caminhoFoto; // caminho absoluto da foto (pode ser null)
    private final String dataCriacao; // data/hora em que o lugar foi salvo

    // Construtor usado ao INSERIR (o id ainda não existe)
    public Lugar(String nome, double latitude, double longitude, String endereco,
                 String caminhoFoto, String dataCriacao) {
        this.nome = nome;
        this.latitude = latitude;
        this.longitude = longitude;
        this.endereco = endereco;
        this.caminhoFoto = caminhoFoto;
        this.dataCriacao = dataCriacao;
    }

    // Construtor usado ao LER do banco (o id já existe)
    public Lugar(long id, String nome, double latitude, double longitude, String endereco,
                 String caminhoFoto, String dataCriacao) {
        this(nome, latitude, longitude, endereco, caminhoFoto, dataCriacao);
        this.id = id;
    }

    public boolean temFoto() {
        return caminhoFoto != null && !caminhoFoto.isEmpty();
    }

    public boolean temEndereco() {
        return endereco != null && !endereco.isEmpty();
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getEndereco() { return endereco; }
    public String getCaminhoFoto() { return caminhoFoto; }
    public String getDataCriacao() { return dataCriacao; }

    public void setNome(String nome) { this.nome = nome; }
}
