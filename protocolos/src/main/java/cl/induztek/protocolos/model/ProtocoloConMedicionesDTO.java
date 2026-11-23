package cl.induztek.protocolos.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ProtocoloConMedicionesDTO {

    @Valid
    @NotNull
    private ProtocoloDTO protocolo;

    @Valid
    private List<MedicionDTO> mediciones = new ArrayList<>();
}
