package br.edu.aula.meuslugares;

public class Lugar {

    private long id;
    private String nome;
    private final double latitude;
    private final double longitude;
    private final Double precisao; // metros, null se o aparelho não informou
    private final String endereco;
    private final String caminhoFoto;
    private final String dataCriacao;

    public Lugar(String nome, double latitude, double longitude, Double precisao,
                 String endereco, String caminhoFoto, String dataCriacao) {
        this.nome = nome;
        this.latitude = latitude;
        this.longitude = longitude;
        this.precisao = precisao;
        this.endereco = endereco;
        this.caminhoFoto = caminhoFoto;
        this.dataCriacao = dataCriacao;
    }

    public Lugar(long id, String nome, double latitude, double longitude, Double precisao,
                 String endereco, String caminhoFoto, String dataCriacao) {
        this(nome, latitude, longitude, precisao, endereco, caminhoFoto, dataCriacao);
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
    public Double getPrecisao() { return precisao; }
    public String getEndereco() { return endereco; }
    public String getCaminhoFoto() { return caminhoFoto; }
    public String getDataCriacao() { return dataCriacao; }

    public void setNome(String nome) { this.nome = nome; }
}
