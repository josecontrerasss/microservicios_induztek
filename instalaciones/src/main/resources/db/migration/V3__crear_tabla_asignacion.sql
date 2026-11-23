CREATE TABLE asignacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_cliente BIGINT NOT NULL,
    nombre_cliente VARCHAR(150) NOT NULL,
    id_tecnico BIGINT NOT NULL,
    nombre_tecnico VARCHAR(150) NOT NULL,
    fecha_asignacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_asignacion_cliente_tecnico UNIQUE (id_cliente, id_tecnico),
    INDEX idx_asignacion_tecnico (id_tecnico)
);

INSERT INTO asignacion (id, id_cliente, nombre_cliente, id_tecnico, nombre_tecnico, fecha_asignacion) VALUES (1, 10, 'Minera del Norte SpA', 1, 'Juan Pérez', NOW(6));

ALTER TABLE asignacion AUTO_INCREMENT = 100;
