package br.edu.aula.meuslugares;

import java.util.Locale;

/** Sem dependência do Android, para o teste rodar na JVM. */
public final class GeoUtils {

    private static final double RAIO_TERRA_KM = 6371.0088;

    private GeoUtils() {
    }

    /** Haversine: linha reta, não o caminho pelas ruas. */
    public static double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * RAIO_TERRA_KM * Math.asin(Math.sqrt(h));
    }

    public static String formatarDistancia(double km, Locale locale) {
        if (km < 1) {
            return String.format(locale, "%.0f m", km * 1000);
        }
        return String.format(locale, "%.2f km", km);
    }
}
