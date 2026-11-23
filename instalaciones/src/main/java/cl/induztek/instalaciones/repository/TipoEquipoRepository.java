package cl.induztek.instalaciones.repository;

import cl.induztek.instalaciones.model.TipoEquipo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoEquipoRepository extends JpaRepository<TipoEquipo, Long> {

    List<TipoEquipo> findAllByOrderByNombreAsc();
}
