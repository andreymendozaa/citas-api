-- HU-036 (WF-003): simulación de un día operativo con datos SINTÉTICOS en la BD de desarrollo.
-- La API no permite crear disponibilidad para el mismo día, así que estas filas se insertan directamente.
-- Solo cuentas lab.* y profesionales de laboratorio. Todas las filas llevan reason = 'SIMULACION HU-036'.
-- No crean professional_slots: WF-003 solo lee /admin/appointments/upcoming y /admin/inbox.
-- Uso:     mysql <db_dev> < scripts/hu036-simulacion-dia.sql
-- Limpieza: ver el bloque final (comentado).

SET @patient  := (SELECT id FROM users WHERE email='lab.paciente@citas.test');
SET @patient2 := (SELECT id FROM users WHERE email='lab.usuario@citas.test');
SET @admin    := (SELECT id FROM users WHERE email='lab.admin@citas.test');
SET @approved := (SELECT id FROM appointment_statuses WHERE code='APPROVED');
SET @requested:= (SELECT id FROM appointment_statuses WHERE code='REQUESTED');
SET @hic := (SELECT id FROM locations WHERE code='HIC');
SET @icv := (SELECT id FROM locations WHERE code='ICV');
SET @prof_gen := (SELECT p.id FROM professionals p JOIN users u ON u.id=p.user_id WHERE u.email='lab.general@citas.test');
SET @prof_car := (SELECT p.id FROM professionals p JOIN users u ON u.id=p.user_id WHERE u.email='lab.cardio@citas.test');
SET @prof_der := (SELECT p.id FROM professionals p JOIN users u ON u.id=p.user_id WHERE u.email='lab.derma@citas.test');
SET @sp_gen := (SELECT specialty_id FROM professional_specialties WHERE professional_id=@prof_gen AND is_primary LIMIT 1);
SET @sp_car := (SELECT specialty_id FROM professional_specialties WHERE professional_id=@prof_car AND is_primary LIMIT 1);
SET @sp_der := (SELECT specialty_id FROM professional_specialties WHERE professional_id=@prof_der AND is_primary LIMIT 1);

-- Three APPROVED appointments today (two sites, three specialties), later than now.
INSERT INTO appointments(patient_user_id,professional_id,location_id,specialty_id,status_id,reason,scheduled_start_at,scheduled_end_at,created_by_user_id,approved_by_user_id,approved_at)
SELECT * FROM (
  SELECT @patient AS p, @prof_gen AS pr, @hic AS l, @sp_gen AS sp, @approved AS st, 'SIMULACION HU-036' AS r, TIMESTAMP(CURDATE(),'15:00:00') AS s, TIMESTAMP(CURDATE(),'15:30:00') AS e, @patient AS cb, NULL AS ab, NULL AS aa UNION ALL
  SELECT @patient2, @prof_der, @hic, @sp_der, @approved, 'SIMULACION HU-036', TIMESTAMP(CURDATE(),'16:00:00'), TIMESTAMP(CURDATE(),'17:00:00'), @patient2, @admin, NOW() UNION ALL
  SELECT @patient, @prof_car, @icv, @sp_car, @approved, 'SIMULACION HU-036', TIMESTAMP(CURDATE(),'17:00:00'), TIMESTAMP(CURDATE(),'18:00:00'), @patient, @admin, NOW()
) t WHERE NOT EXISTS (SELECT 1 FROM appointments WHERE reason='SIMULACION HU-036' AND DATE(scheduled_start_at)=CURDATE());

-- One pending specialized request (tomorrow) for the ADMIN inbox.
INSERT INTO appointments(patient_user_id,professional_id,location_id,specialty_id,status_id,reason,scheduled_start_at,scheduled_end_at,created_by_user_id)
SELECT @patient2, @prof_car, @icv, @sp_car, @requested, 'SIMULACION HU-036 pendiente', TIMESTAMP(CURDATE() + INTERVAL 1 DAY,'11:00:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY,'12:00:00'), @patient2
WHERE NOT EXISTS (SELECT 1 FROM appointments WHERE reason='SIMULACION HU-036 pendiente');

-- Audit history consistent with how the API records each state.
INSERT INTO appointment_status_history(appointment_id,status_id,changed_by_user_id,change_source,reason)
SELECT a.id, a.status_id, COALESCE(a.approved_by_user_id, a.patient_user_id),
       CASE WHEN a.approved_by_user_id IS NOT NULL THEN 'ADMIN' WHEN a.status_id=@requested THEN 'USER' ELSE 'SYSTEM' END,
       'SIMULACION HU-036'
FROM appointments a
WHERE a.reason LIKE 'SIMULACION HU-036%' AND NOT EXISTS (SELECT 1 FROM appointment_status_history h WHERE h.appointment_id=a.id);

-- One pending reschedule request on today's dermatology appointment.
INSERT INTO reschedule_requests(appointment_id,requested_by_user_id,requested_location_id,status_id,previous_start_at,previous_end_at,requested_start_at,requested_end_at)
SELECT a.id, a.patient_user_id, a.location_id, (SELECT id FROM reschedule_request_statuses WHERE code='PENDING'),
       a.scheduled_start_at, a.scheduled_end_at, a.scheduled_start_at + INTERVAL 2 DAY, a.scheduled_end_at + INTERVAL 2 DAY
FROM appointments a
WHERE a.reason='SIMULACION HU-036' AND a.professional_id=@prof_der AND DATE(a.scheduled_start_at)=CURDATE()
  AND NOT EXISTS (SELECT 1 FROM reschedule_requests r WHERE r.appointment_id=a.id);

SELECT a.id, l.code, s.name, st.code, a.scheduled_start_at FROM appointments a
JOIN locations l ON l.id=a.location_id JOIN specialties s ON s.id=a.specialty_id JOIN appointment_statuses st ON st.id=a.status_id
WHERE a.reason LIKE 'SIMULACION HU-036%' ORDER BY a.scheduled_start_at;

-- Limpieza (ejecutar manualmente cuando ya no se necesite la simulación):
-- DELETE r FROM reschedule_requests r JOIN appointments a ON a.id=r.appointment_id WHERE a.reason LIKE 'SIMULACION HU-036%';
-- DELETE h FROM appointment_status_history h JOIN appointments a ON a.id=h.appointment_id WHERE a.reason LIKE 'SIMULACION HU-036%';
-- DELETE FROM appointments WHERE reason LIKE 'SIMULACION HU-036%';
