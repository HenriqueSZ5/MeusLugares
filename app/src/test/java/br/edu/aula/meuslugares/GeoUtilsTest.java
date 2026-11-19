package br.edu.aula.meuslugares;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Locale;

// Valores esperados: tabela da seção 8.2 da apostila de GPS
public class GeoUtilsTest {

    private static final double PELOURINHO_LAT = -12.9714;
    private static final double PELOURINHO_LNG = -38.5124;

    @Test
    public void pelourinhoAoFarolDaBarra() {
        double km = GeoUtils.distanciaKm(PELOURINHO_LAT, PELOURINHO_LNG, -13.0105, -38.5326);
        assertEquals(4.87, km, 0.005);
    }

    @Test
    public void pelourinhoAFeiraDeSantana() {
        double km = GeoUtils.distanciaKm(PELOURINHO_LAT, PELOURINHO_LNG, -12.2664, -38.9663);
        assertEquals(92.58, km, 0.005);
    }

    @Test
    public void mesmoPontoDaZero() {
        assertEquals(0, GeoUtils.distanciaKm(PELOURINHO_LAT, PELOURINHO_LNG, PELOURINHO_LAT, PELOURINHO_LNG), 1e-9);
    }

    @Test
    public void formataMetrosEQuilometros() {
        Locale ptBr = Locale.forLanguageTag("pt-BR");
        assertEquals("220 m", GeoUtils.formatarDistancia(0.22, ptBr));
        assertEquals("4,87 km", GeoUtils.formatarDistancia(4.8712, ptBr));
    }
}
