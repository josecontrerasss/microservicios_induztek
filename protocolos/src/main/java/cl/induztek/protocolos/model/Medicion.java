package cl.induztek.protocolos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "medicion")
public class Medicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_protocolo", nullable = false)
    private Protocolo protocolo;

    @Column(name = "parametro_codigo", nullable = false, length = 30)
    private String parametroCodigo;

    @Column(name = "parametro_nombre", nullable = false, length = 100)
    private String parametroNombre;

    @Column(nullable = false, length = 30)
    private String valor;

    @Column(nullable = false, length = 20)
    private String unidad;

    @Column(name = "estado_alerta", nullable = false, length = 20)
    private String estadoAlerta;
}
