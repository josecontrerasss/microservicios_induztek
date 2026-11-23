package cl.induztek.protocolos.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncRespuestaDTO {

    private int sincronizados;

    private List<String> aceptados;

    private List<String> rechazados;
}
