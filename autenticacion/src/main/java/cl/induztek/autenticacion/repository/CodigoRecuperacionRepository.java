package cl.induztek.autenticacion.repository;

import cl.induztek.autenticacion.model.CodigoRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    Optional<CodigoRecuperacion> findFirstByIdUsuarioAndUsadoFalseOrderByIdDesc(Long idUsuario);
}
