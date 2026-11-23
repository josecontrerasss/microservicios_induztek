CREATE TABLE protocolo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(30) NOT NULL,
    id_visita BIGINT NOT NULL,
    id_equipo BIGINT NOT NULL,
    id_instalacion BIGINT NOT NULL,
    id_cliente BIGINT NOT NULL,
    id_tecnico BIGINT NOT NULL,
    fecha_hora_prueba VARCHAR(40) NOT NULL,
    temperatura_c DOUBLE,
    humedad_pct DOUBLE,
    descripcion_mantenimiento VARCHAR(1000),
    observaciones VARCHAR(1000),
    estado VARCHAR(30) NOT NULL,
    resultado VARCHAR(30) NOT NULL,
    foto_path VARCHAR(255),
    fecha_sincronizacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_protocolo_numero UNIQUE (numero),
    INDEX idx_protocolo_cliente (id_cliente),
    INDEX idx_protocolo_tecnico (id_tecnico)
);

CREATE TABLE medicion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_protocolo BIGINT NOT NULL,
    parametro_codigo VARCHAR(30) NOT NULL,
    parametro_nombre VARCHAR(100) NOT NULL,
    valor VARCHAR(30) NOT NULL,
    unidad VARCHAR(20) NOT NULL,
    estado_alerta VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_medicion_protocolo FOREIGN KEY (id_protocolo) REFERENCES protocolo (id) ON DELETE CASCADE
);
