package cl.induztek.autenticacion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RutValidadorTest {

    @Test
    @DisplayName("Un RUT con dígito verificador correcto es válido")
    void rutConDvCorrectoEsValido() {
        assertThat(RutValidador.esValido("12345678-5")).isTrue();
        assertThat(RutValidador.esValido("11111112-K")).isTrue();
    }

    @Test
    @DisplayName("Un RUT con dígito verificador incorrecto es inválido")
    void rutConDvIncorrectoEsInvalido() {
        assertThat(RutValidador.esValido("12345678-9")).isFalse();
    }

    @Test
    @DisplayName("Un RUT con cuerpo no numérico es inválido")
    void rutConCuerpoNoNumericoEsInvalido() {
        assertThat(RutValidador.esValido("ABC45678-5")).isFalse();
    }

    @Test
    @DisplayName("Un RUT null o demasiado corto es inválido")
    void rutNullOCortoEsInvalido() {
        assertThat(RutValidador.esValido(null)).isFalse();
        assertThat(RutValidador.esValido("1")).isFalse();
    }

    @Test
    @DisplayName("Formatear un RUT limpio agrega puntos y guion")
    void formatearAgregaPuntosYGuion() {
        assertThat(RutValidador.formatear("12345678-5")).isEqualTo("12.345.678-5");
        assertThat(RutValidador.formatear("123456785")).isEqualTo("12.345.678-5");
        assertThat(RutValidador.formatear("11111112-K")).isEqualTo("11.111.112-K");
    }
}
