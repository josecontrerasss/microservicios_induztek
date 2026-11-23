package cl.induztek.instalaciones.repository;

import cl.induztek.instalaciones.model.Instalacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstalacionRepository extends JpaRepository<Instalacion, Long> {

    List<Instalacion> findByIdClienteOrderByNombreAsc(Long idCliente);

    boolean existsByCodigo(String codigo);

    @Query("SELECT i FROM Instalacion i WHERE i.id IN (SELECT v.idInstalacion FROM Visita v WHERE v.idTecnico = :idTecnico) ORDER BY i.nombre")
    List<Instalacion> findAsignadasATecnico(@Param("idTecnico") Long idTecnico);
}
