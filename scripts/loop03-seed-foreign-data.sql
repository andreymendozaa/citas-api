-- LOOP_03 (pruebas frágiles): siembra datos "ajenos" persistentes en la BD de pruebas, como los que deja
-- una corrida interrumpida u otra suite: una solicitud especializada REQUESTED y una reprogramación PENDING
-- que ninguna prueba resuelve. Idempotente (prefijo LOOP03). Solo para la BD de pruebas, nunca para la de desarrollo.
-- Uso: mysql <db_de_pruebas> < scripts/loop03-seed-foreign-data.sql

INSERT IGNORE INTO users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified)
VALUES ('Loop03','Paciente','CC','LOOP03-PAC','loop03.paciente@example.test','3000000000','hash',TRUE,FALSE),
       ('Loop03','Profesional','CC','LOOP03-PRO','loop03.profesional@example.test','3000000000','hash',TRUE,FALSE);

INSERT IGNORE INTO professionals(user_id,professional_code,license_number,active)
SELECT id,'LOOP03-PRO','LOOP03-LIC',TRUE FROM users WHERE email='loop03.profesional@example.test';

INSERT IGNORE INTO specialties(code,name,appointment_duration_minutes,is_general,requires_admin_approval,active)
VALUES ('LOOP03_ESP','Loop03 Especializada',60,FALSE,TRUE,TRUE);

-- REQUESTED specialized appointment that nobody resolves.
INSERT INTO appointments(patient_user_id,professional_id,location_id,specialty_id,status_id,reason,scheduled_start_at,scheduled_end_at,created_by_user_id)
SELECT u.id,p.id,(SELECT id FROM locations WHERE active=TRUE ORDER BY id LIMIT 1),s.id,st.id,'LOOP03 ajena REQUESTED',
       TIMESTAMP(CURDATE() + INTERVAL 30 DAY,'07:00:00'),TIMESTAMP(CURDATE() + INTERVAL 30 DAY,'08:00:00'),u.id
FROM users u, professionals p, specialties s, appointment_statuses st
WHERE u.email='loop03.paciente@example.test' AND p.professional_code='LOOP03-PRO' AND s.code='LOOP03_ESP' AND st.code='REQUESTED'
  AND NOT EXISTS (SELECT 1 FROM appointments a WHERE a.reason='LOOP03 ajena REQUESTED');

-- Second foreign REQUESTED, earlier than any test date, so it sorts first in global pending lists.
INSERT INTO appointments(patient_user_id,professional_id,location_id,specialty_id,status_id,reason,scheduled_start_at,scheduled_end_at,created_by_user_id)
SELECT u.id,p.id,(SELECT id FROM locations WHERE active=TRUE ORDER BY id LIMIT 1),s.id,st.id,'LOOP03 ajena REQUESTED temprana',
       TIMESTAMP(CURDATE() + INTERVAL 1 DAY,'06:00:00'),TIMESTAMP(CURDATE() + INTERVAL 1 DAY,'07:00:00'),u.id
FROM users u, professionals p, specialties s, appointment_statuses st
WHERE u.email='loop03.paciente@example.test' AND p.professional_code='LOOP03-PRO' AND s.code='LOOP03_ESP' AND st.code='REQUESTED'
  AND NOT EXISTS (SELECT 1 FROM appointments a WHERE a.reason='LOOP03 ajena REQUESTED temprana');

-- APPROVED appointment with a PENDING reschedule request that nobody resolves.
INSERT INTO appointments(patient_user_id,professional_id,location_id,specialty_id,status_id,reason,scheduled_start_at,scheduled_end_at,created_by_user_id)
SELECT u.id,p.id,(SELECT id FROM locations WHERE active=TRUE ORDER BY id LIMIT 1),s.id,st.id,'LOOP03 ajena APPROVED',
       TIMESTAMP(CURDATE() + INTERVAL 31 DAY,'07:00:00'),TIMESTAMP(CURDATE() + INTERVAL 31 DAY,'08:00:00'),u.id
FROM users u, professionals p, specialties s, appointment_statuses st
WHERE u.email='loop03.paciente@example.test' AND p.professional_code='LOOP03-PRO' AND s.code='LOOP03_ESP' AND st.code='APPROVED'
  AND NOT EXISTS (SELECT 1 FROM appointments a WHERE a.reason='LOOP03 ajena APPROVED');

INSERT INTO reschedule_requests(appointment_id,requested_by_user_id,requested_location_id,status_id,previous_start_at,previous_end_at,requested_start_at,requested_end_at)
SELECT a.id,a.patient_user_id,a.location_id,rs.id,a.scheduled_start_at,a.scheduled_end_at,
       a.scheduled_start_at + INTERVAL 1 DAY,a.scheduled_end_at + INTERVAL 1 DAY
FROM appointments a, reschedule_request_statuses rs
WHERE a.reason='LOOP03 ajena APPROVED' AND rs.code='PENDING'
  AND NOT EXISTS (SELECT 1 FROM reschedule_requests r WHERE r.appointment_id=a.id);

-- Restore the intended state in case a fragile test consumed the foreign data (e.g. decided it).
UPDATE appointments SET status_id=(SELECT id FROM appointment_statuses WHERE code='REQUESTED'), approved_by_user_id=NULL, approved_at=NULL
WHERE reason LIKE 'LOOP03 ajena REQUESTED%';
UPDATE reschedule_requests SET status_id=(SELECT id FROM reschedule_request_statuses WHERE code='PENDING'), decision_reason=NULL, decided_by_user_id=NULL, decided_at=NULL
WHERE appointment_id IN (SELECT id FROM (SELECT id FROM appointments WHERE reason='LOOP03 ajena APPROVED') x);

SELECT 'LOOP03 foreign REQUESTED' AS item, COUNT(*) AS n FROM appointments WHERE reason LIKE 'LOOP03 ajena REQUESTED%'
UNION ALL
SELECT 'LOOP03 foreign PENDING reschedule', COUNT(*) FROM reschedule_requests r JOIN appointments a ON a.id=r.appointment_id WHERE a.reason='LOOP03 ajena APPROVED';
