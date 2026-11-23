package cl.induztek.protocolos.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "protocolo")
public class Protocolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String numero;

    @Column(name = "id_visita", nullable = false)
    private Long idVisita;

    @Column(name = "id_equipo", nullable = false)
    private Long idEquipo;

    @Column(name = "id_instalacion", nullable = false)
    private Long idInstalacion;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    @Column(name = "id_tecnico", nullable = false)
    private Long idTecnico;

    @Column(name = "fecha_hora_prueba", nullable = false, length = 40)
    private String fechaHoraPrueba;

    @Column(name = "temperatura_c")
    private Double temperaturaC;

    @Column(name = "humedad_pct")
    private Double humedadPct;

    @Column(name = "descripcion_mantenimiento", length = 1000)
    private String descripcionMantenimiento;

    @Column(length = 1000)
    private String observaciones;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(nullable = false, length = 30)
    private String resultado;

    @Column(name = "foto_path", length = 255)
    private String fotoPath;

    @Column(name = "fecha_sincronizacion", nullable = false, updatable = false)
    private LocalDateTime fechaSincronizacion;

    @OneToMany(mappedBy = "protocolo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Medicion> mediciones = new ArrayList<>();

    @PrePersist
    protected void asignarFechaSincronizacion() {
        this.fechaSincronizacion = LocalDateTime.now();
    }

    public void agregarMedicion(Medicion medicion) {
        medicion.setProtocolo(this);
        this.mediciones.add(medicion);
    }
}
