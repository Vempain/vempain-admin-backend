-- Service-to-service API tokens presented by other services (the file backend) in the X-Vempain-Api-Token header. Only the SHA-256
-- hash of a token is stored; the token string is shown once when it is created. Deleting a row revokes the token.
CREATE TABLE api_token
(
	id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	token_hash    VARCHAR(64)  NOT NULL UNIQUE,
	token_prefix  VARCHAR(16)  NOT NULL,
	description   VARCHAR(255) NOT NULL,
	network       VARCHAR(64)  NOT NULL,
	expires_at    TIMESTAMP    NOT NULL,
	owner_user_id BIGINT       NOT NULL,
	created_by    BIGINT       NOT NULL,
	created       TIMESTAMP    NOT NULL,
	last_used     TIMESTAMP,
	FOREIGN KEY (owner_user_id) REFERENCES user_account (id),
	FOREIGN KEY (created_by) REFERENCES user_account (id)
);
