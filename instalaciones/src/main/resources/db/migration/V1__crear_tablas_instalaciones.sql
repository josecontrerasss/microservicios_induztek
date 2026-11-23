CREATE TABLE tipo_equipo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    clasificacion VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tipo_equipo_nombre UNIQUE (nombre)
);

CREATE TABLE instalacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_cliente BIGINT NOT NULL,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    ubicacion VARCHAR(255) NOT NULL,
    observaciones_acceso VARCHAR(255),
    ultima_actividad VARCHAR(30),
    PRIMARY KEY (id),
    CONSTRAINT uk_instalacion_codigo UNIQUE (codigo),
    INDEX idx_instalacion_cliente (id_cliente)
);

CREATE TABLE equipo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_instalacion BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    id_tipo_equipo BIGINT NOT NULL,
    marca VARCHAR(100) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    numero_serie VARCHAR(100) NOT NULL,
    anio_fabricacion INT,
    potencia_kva DOUBLE,
    tension_primaria_kv DOUBLE,
    tension_secundaria_kv DOUBLE,
    corriente_nominal_a DOUBLE,
    observaciones VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_equipo_instalacion_tag UNIQUE (id_instalacion, tag),
    CONSTRAINT fk_equipo_instalacion FOREIGN KEY (id_instalacion) REFERENCES instalacion (id) ON DELETE CASCADE,
    CONSTRAINT fk_equipo_tipo FOREIGN KEY (id_tipo_equipo) REFERENCES tipo_equipo (id)
);

CREATE TABLE visita (
    id BIGINT NOT NULL AUTO_INCREMENT,
    numero_ot VARCHAR(30) NOT NULL,
    id_instalacion BIGINT NOT NULL,
    fecha VARCHAR(20) NOT NULL,
    tipo_visita VARCHAR(30) NOT NULL,
    id_tecnico BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_visita_numero_ot UNIQUE (numero_ot),
    CONSTRAINT fk_visita_instalacion FOREIGN KEY (id_instalacion) REFERENCES instalacion (id) ON DELETE CASCADE,
    INDEX idx_visita_tecnico (id_tecnico)
);
