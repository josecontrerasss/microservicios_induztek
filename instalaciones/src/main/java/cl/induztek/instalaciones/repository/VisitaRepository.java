package cl.induztek.instalaciones.repository;

import cl.induztek.instalaciones.model.Visita;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitaRepository extends JpaRepository<Visita, Long> {

    List<Visita> findByIdTecnicoOrderByIdDesc(Long idTecnico);

    List<Visita> findByIdInstalacionInOrderByIdDesc(Collection<Long> idsInstalacion);

    boolean existsByIdInstalacionAndIdTecnico(Long idInstalacion, Long idTecnico);
}
