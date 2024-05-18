-- Correct the acl permission id sequence
ALTER SEQUENCE acl_permission_id_seq RESTART WITH 5;

ALTER SEQUENCE user_account_id_seq RESTART WITH 2;

ALTER SEQUENCE unit_id_seq RESTART WITH 4;
