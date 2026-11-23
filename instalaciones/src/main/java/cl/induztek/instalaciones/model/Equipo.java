package cl.induztek.instalaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "equipo")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_instalacion", nullable = false)
    private Long idInstalacion;

    @Column(nullable = false, length = 100)
    private String tag;

    @Column(name = "id_tipo_equipo", nullable = false)
    private Long idTipoEquipo;

    @Column(nullable = false, length = 100)
    private String marca;

    @Column(nullable = false, length = 100)
    private String modelo;

    @Column(name = "numero_serie", nullable = false, length = 100)
    private String numeroSerie;

    @Column(name = "anio_fabricacion")
    private Integer anioFabricacion;

    @Column(name = "potencia_kva")
    private Double potenciaKva;

    @Column(name = "tension_primaria_kv")
    private Double tensionPrimariaKv;

    @Column(name = "tension_secundaria_kv")
    private Double tensionSecundariaKv;

    @Column(name = "corriente_nominal_a")
    private Double corrienteNominalA;

    @Column(length = 255)
    private String observaciones;
}
