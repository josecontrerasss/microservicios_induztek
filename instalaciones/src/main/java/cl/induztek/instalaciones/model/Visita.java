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
@Table(name = "visita")
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ot", nullable = false, length = 30, unique = true)
    private String numeroOt;

    @Column(name = "id_instalacion", nullable = false)
    private Long idInstalacion;

    @Column(nullable = false, length = 20)
    private String fecha;

    @Column(name = "tipo_visita", nullable = false, length = 30)
    private String tipoVisita;

    @Column(name = "id_tecnico")
    private Long idTecnico;
}
