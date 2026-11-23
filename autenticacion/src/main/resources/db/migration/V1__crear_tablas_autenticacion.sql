CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rut VARCHAR(12) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    correo VARCHAR(255) NOT NULL,
    contrasena VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    rol VARCHAR(20) NOT NULL,
    activo BIT(1) NOT NULL,
    giro VARCHAR(150),
    contacto_nombre VARCHAR(150),
    contacto_cargo VARCHAR(150),
    fecha_creacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_rut UNIQUE (rut),
    CONSTRAINT uk_usuario_correo UNIQUE (correo)
);

CREATE TABLE codigo_recuperacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario BIGINT NOT NULL,
    codigo VARCHAR(6) NOT NULL,
    expira_en DATETIME(6) NOT NULL,
    usado BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_codigo_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id) ON DELETE CASCADE
);
