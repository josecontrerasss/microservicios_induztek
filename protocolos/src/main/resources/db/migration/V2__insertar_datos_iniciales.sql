INSERT INTO protocolo (id, numero, id_visita, id_equipo, id_instalacion, id_cliente, id_tecnico, fecha_hora_prueba, temperatura_c, humedad_pct, descripcion_mantenimiento, observaciones, estado, resultado, foto_path, fecha_sincronizacion) VALUES
(1, 'PT-2025-0008', 2, 3, 2, 10, 1, '15 may. 2025, 09:15', 21.5, 42.0, 'Pruebas de aislación y relación de transformación.', 'Equipo operando en condiciones normales. Parámetros conformes.', 'FORMALIZADO', 'APROBADO', NULL, NOW(6)),
(2, 'PT-2025-0007', 1, 2, 1, 10, 1, '10 may. 2025, 14:30', 24.0, 40.0, 'Verificación de resistencia de contactos.', 'Contactos dentro de norma IEEE.', 'FORMALIZADO', 'APROBADO', NULL, NOW(6));

INSERT INTO medicion (id, id_protocolo, parametro_codigo, parametro_nombre, valor, unidad, estado_alerta) VALUES
(1, 1, 'V_PRIM', 'Tensión Primario', '23000', 'V', 'OK'),
(2, 1, 'V_SEC', 'Tensión Secundario', '400', 'V', 'OK'),
(3, 1, 'I_NOM', 'Corriente', '378.2', 'A', 'OK'),
(4, 1, 'R_AISL', 'Resistencia Aislamiento', '> 1000', 'MΩ', 'OK'),
(5, 1, 'T_ACEITE', 'Temperatura Aceite', '41.5', '°C', 'OK');

ALTER TABLE protocolo AUTO_INCREMENT = 100;
ALTER TABLE medicion AUTO_INCREMENT = 100;
