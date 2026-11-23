package cl.induztek.protocolos.repository;

import cl.induztek.protocolos.model.Protocolo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProtocoloRepository extends JpaRepository<Protocolo, Long> {

    List<Protocolo> findByIdClienteOrderByIdDesc(Long idCliente);

    List<Protocolo> findByIdTecnicoOrderByIdDesc(Long idTecnico);

    Optional<Protocolo> findByNumero(String numero);

    List<Protocolo> findByEstadoOrderByIdDesc(String estado);
}
