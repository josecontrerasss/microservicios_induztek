package cl.induztek.instalaciones.repository;

import cl.induztek.instalaciones.model.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    boolean existsByIdClienteAndIdTecnico(Long idCliente, Long idTecnico);

    List<Asignacion> findAllByOrderByIdDesc();

    List<Asignacion> findByIdClienteOrderByNombreTecnicoAsc(Long idCliente);

    List<Asignacion> findByIdTecnicoOrderByNombreClienteAsc(Long idTecnico);
}
