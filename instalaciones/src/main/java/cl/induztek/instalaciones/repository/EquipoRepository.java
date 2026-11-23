package cl.induztek.instalaciones.repository;

import cl.induztek.instalaciones.model.Equipo;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    List<Equipo> findByIdInstalacionOrderByTagAsc(Long idInstalacion);

    boolean existsByIdInstalacionAndTag(Long idInstalacion, String tag);

    List<Equipo> findByIdInstalacionInOrderByTagAsc(Collection<Long> idsInstalacion);
}
