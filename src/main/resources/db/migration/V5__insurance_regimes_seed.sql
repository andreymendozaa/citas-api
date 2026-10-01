-- HU-011 / RF-05: complete the fixed insurance-regime catalog with the values of the trainer's reference model
-- (database/reference/db.sql), approved by the user on 2026-09-30. Seed only: no schema change, so 3FN is unaffected
-- (regimes stay a fixed catalog referenced by eps_plans.regime_id). Idempotent by the unique `code`.
INSERT INTO insurance_regimes(code, name) VALUES
  ('CONTRIBUTIVO', 'Contributivo'),
  ('SUBSIDIADO', 'Subsidiado'),
  ('ESPECIAL', 'Especial'),
  ('EXCEPCION', 'Excepción'),
  ('PARTICULAR', 'Particular')
ON DUPLICATE KEY UPDATE name = VALUES(name);
