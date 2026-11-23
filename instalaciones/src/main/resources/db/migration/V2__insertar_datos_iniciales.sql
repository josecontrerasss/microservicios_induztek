INSERT INTO tipo_equipo (id, nombre, clasificacion) VALUES
(1, 'Transformador', 'PRIMARIO'),
(2, 'Interruptor', 'PRIMARIO'),
(3, 'Celda MT', 'PRIMARIO');

INSERT INTO instalacion (id, id_cliente, codigo, nombre, ubicacion, observaciones_acceso, ultima_actividad) VALUES
(1, 10, 'SE-IND-102', 'Planta Norte', 'Km 45 Ruta 5 Norte, Lampa', 'Inducción básica de seguridad requerida al ingreso', '15 may. 2026'),
(2, 10, 'SE-MDN-01', 'Planta Concentradora', 'Patio 110 kV, Edificio Concentradora', 'EPP completo y permiso de trabajo en altura', '15 may. 2026'),
(3, 10, 'SE-MDN-02', 'Planta Chancado', 'Sector Chancado Primario', 'Coordinar con operador de sala de control', '10 may. 2026');

INSERT INTO equipo (id, id_instalacion, tag, id_tipo_equipo, marca, modelo, numero_serie, anio_fabricacion, potencia_kva, tension_primaria_kv, tension_secundaria_kv, corriente_nominal_a, observaciones) VALUES
(1, 1, 'Transformador T-01', 1, 'ABB', 'Resibloc 1000', 'ABB-TR-9982', 2021, 1000.0, 13.2, 0.42, 256.5, ''),
(2, 1, 'Interruptor INT-01', 2, 'Schneider', 'Evolis 24kV', 'SCH-SW-4412', 2021, 0.0, 24.0, 0.0, 630.0, ''),
(3, 2, 'Transformador T-02', 1, 'Siemens', 'Geafol 1500', 'SIE-TR-1123', 2021, 1500.0, 23.0, 0.40, 380.0, '');

INSERT INTO visita (id, numero_ot, id_instalacion, fecha, tipo_visita, id_tecnico) VALUES
(1, 'OT-2026-1187', 1, '15-05-2026', 'PRUEBAS_Y_MANT', 1),
(2, 'OT-2026-1140', 2, '10-05-2026', 'SOLO_PRUEBAS', 1);

ALTER TABLE tipo_equipo AUTO_INCREMENT = 100;
ALTER TABLE instalacion AUTO_INCREMENT = 100;
ALTER TABLE equipo AUTO_INCREMENT = 100;
ALTER TABLE visita AUTO_INCREMENT = 100;
