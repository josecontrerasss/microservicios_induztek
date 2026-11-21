package cl.induztek.autenticacion.service;

public final class RutValidador {

    private RutValidador() {
    }

    public static boolean esValido(String rut) {
        String limpio = limpiar(rut);
        if (limpio.length() < 2) {
            return false;
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        if (!cuerpo.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return calcularDv(cuerpo) == dv;
    }

    public static String formatear(String rut) {
        String limpio = limpiar(rut);
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        StringBuilder invertido = new StringBuilder(cuerpo).reverse();
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < invertido.length(); i++) {
            if (i > 0 && i % 3 == 0) {
                resultado.append('.');
            }
            resultado.append(invertido.charAt(i));
        }
        return resultado.reverse() + "-" + dv;
    }

    private static String limpiar(String rut) {
        return rut == null ? "" : rut.replaceAll("[^0-9kK]", "").toUpperCase();
    }

    private static char calcularDv(String cuerpo) {
        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return (char) ('0' + resto);
    }
}
