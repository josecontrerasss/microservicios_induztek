INSERT INTO usuario (id, rut, nombre, correo, contrasena, telefono, rol, activo, giro, contacto_nombre, contacto_cargo, fecha_creacion) VALUES
(1, '16.782.910-4', 'Juan Pérez', 'tecnico@induztek.cl', '$2a$10$qViXvM3HfogiCstBHwUUgOer5H2KZKh/LmXnTuOjtqocB5tiy910K', '+56 9 8765 4321', 'TECNICO', 1, NULL, NULL, NULL, NOW(6)),
(10, '76.543.210-K', 'Minera del Norte SpA', 'cliente@mineranorte.cl', '$2a$10$f7noMisAxQGnzLIn72ixIuIO.M9g8q8Hhv92PbrwMGLb4Blv4shwS', '+56 2 2345 6789', 'CLIENTE', 1, 'Minería y Concentrado de Cobre', 'Carlos Mendoza', 'Jefe Mantenimiento Eléctrico', NOW(6));

ALTER TABLE usuario AUTO_INCREMENT = 100;
