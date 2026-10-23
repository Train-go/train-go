-- Registration requires a phone; existing admin accounts may leave it null.
ALTER TABLE users ADD COLUMN phone_number VARCHAR(15) NULL;

-- Update only the eleven demo customers, preserving any non-seed accounts.
UPDATE users SET phone_number = CASE email
    WHEN 'an.nguyen@example.com' THEN '0901000001'
    WHEN 'binh.tran@example.com' THEN '0901000002'
    WHEN 'cuong.le@example.com' THEN '0901000003'
    WHEN 'dung.pham@example.com' THEN '0901000004'
    WHEN 'duc.vu@example.com' THEN '0901000005'
    WHEN 'lan.hoang@example.com' THEN '0901000006'
    WHEN 'huy.dang@example.com' THEN '0901000007'
    WHEN 'ha.bui@example.com' THEN '0901000008'
    WHEN 'son.ngo@example.com' THEN '0901000009'
    WHEN 'linh.do@example.com' THEN '0901000010'
    WHEN 'bao.ly@example.com' THEN '0901000011'
END
WHERE role = 'CUSTOMER' AND email IN (
    'an.nguyen@example.com', 'binh.tran@example.com', 'cuong.le@example.com',
    'dung.pham@example.com', 'duc.vu@example.com', 'lan.hoang@example.com',
    'huy.dang@example.com', 'ha.bui@example.com', 'son.ngo@example.com',
    'linh.do@example.com', 'bao.ly@example.com'
) AND phone_number IS NULL;
