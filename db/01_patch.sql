-- AgriMatch patch 01: data fixes + schema additions. Run in phpMyAdmin on agrimatch_db.
USE agrimatch_db;

-- 1. Clean stray whitespace in phone numbers
UPDATE farmers SET contact_number = TRIM(REPLACE(contact_number, CHAR(9), ''));
UPDATE buyers  SET contact_number = TRIM(REPLACE(contact_number, CHAR(9), ''));

-- 2. Approximate barangay-centre coordinates (Davao City) so distance works without GPS
UPDATE farmers SET latitude=7.1900000, longitude=125.4550000 WHERE farmer_id=1; -- Calinan
UPDATE farmers SET latitude=7.1100000, longitude=125.5000000 WHERE farmer_id=2; -- Tugbok
UPDATE farmers SET latitude=7.0150000, longitude=125.4950000 WHERE farmer_id=3; -- Toril
UPDATE buyers  SET latitude=7.0600000, longitude=125.5700000 WHERE buyer_id=1;  -- Matina
UPDATE buyers  SET latitude=7.1000000, longitude=125.5700000 WHERE buyer_id=2;  -- Buhangin
UPDATE buyers  SET latitude=7.0500000, longitude=125.5500000 WHERE buyer_id=3;  -- Bangkal

-- 3. Make the sample batches available for matching
UPDATE crop_batches SET status='LISTED' WHERE status='HARVESTED';

-- 4. Offline/low-signal support: how a record was entered, and when it last changed
ALTER TABLE crop_batches
  ADD COLUMN source ENUM('APP','SMS','CALL','COORDINATOR') NOT NULL DEFAULT 'APP' AFTER status,
  ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp();
ALTER TABLE buyer_requests
  ADD COLUMN source ENUM('APP','SMS','CALL','COORDINATOR') NOT NULL DEFAULT 'APP' AFTER status;

-- 5. Phone numbers identify people (needed for SMS)
ALTER TABLE farmers ADD UNIQUE KEY uq_farmer_contact (contact_number);
ALTER TABLE buyers  ADD UNIQUE KEY uq_buyer_contact  (contact_number);

-- 6. Batches with expiry and quantity still free (not held by active reservations)
CREATE OR REPLACE VIEW v_available_batches AS
SELECT b.batch_id, b.farmer_id, b.crop_id, c.crop_name, b.price_per_kg, b.harvest_date,
       DATE_ADD(b.harvest_date, INTERVAL c.shelf_life_days DAY) AS expiry_date,
       DATEDIFF(DATE_ADD(b.harvest_date, INTERVAL c.shelf_life_days DAY), CURDATE()) AS days_left,
       b.quantity - COALESCE((SELECT SUM(r.reserved_quantity) FROM reservations r
                              WHERE r.batch_id=b.batch_id AND r.status IN ('PENDING','CONFIRMED','COMPLETED')),0)
         AS remaining_quantity
FROM crop_batches b JOIN crops c ON c.crop_id=b.crop_id
WHERE b.status IN ('LISTED','RESERVED');
