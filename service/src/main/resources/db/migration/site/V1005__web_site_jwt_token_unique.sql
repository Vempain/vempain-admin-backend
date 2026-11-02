-- The website backend used to record a new session token on every authenticated request, which left one row per request in this
-- table and byte-identical duplicates for the requests of one second. The backend now records a token once and slides the session
-- once per refresh window; the duplicates are reduced to their oldest row here and the token becomes unique so that a regression
-- cannot fill the table again. The expiry index serves the hourly removal of expired tokens and the validity lookups.
DELETE FROM web_site_jwt_token later
USING web_site_jwt_token first
WHERE later.token = first.token
  AND later.id > first.id;

DELETE FROM web_site_jwt_token
WHERE expires <= CURRENT_TIMESTAMP;

CREATE UNIQUE INDEX uq_web_site_jwt_token_token ON web_site_jwt_token (token);
CREATE INDEX idx_web_site_jwt_token_expires ON web_site_jwt_token (expires);
