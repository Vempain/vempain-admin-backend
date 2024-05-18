-- file_common table
-- The converted_file must be unique
ALTER TABLE file_common
	ADD CONSTRAINT file_common_unique_converted_file UNIQUE (converted_file);

-- The acl_id must be unique
ALTER TABLE file_common
	ADD CONSTRAINT file_common_unique_acl_id UNIQUE (acl_id);

-- page table
-- The path must be unique
ALTER TABLE page
	ADD CONSTRAINT page_unique_path UNIQUE (path);

-- The acl_id must be unique
ALTER TABLE page
	ADD CONSTRAINT page_unique_acl_id UNIQUE (acl_id);

-- layout table
-- The layout_name must be unique
ALTER TABLE layout
	ADD CONSTRAINT layout_unique_layout_name UNIQUE (layout_name);

-- The acl_id must be unique
ALTER TABLE layout
	ADD CONSTRAINT layout_unique_acl_id UNIQUE (acl_id);

-- form table
-- The form_name must be unique
ALTER TABLE form
	ADD CONSTRAINT form_unique_form_name UNIQUE (form_name);

-- The acl_id must be unique
ALTER TABLE form
	ADD CONSTRAINT form_unique_acl_id UNIQUE (acl_id);

-- file_thumb table
-- The filepath and filename combination must be unique
ALTER TABLE file_thumb
	ADD CONSTRAINT file_thumb_unique_filepath_filename UNIQUE (filepath, filename);

-- subject table
-- The subject must be unique
ALTER TABLE subject
	ADD CONSTRAINT subject_unique_subject UNIQUE (subject);

-- gallery table
-- The shortname must be unique
ALTER TABLE gallery
	ADD CONSTRAINT gallery_unique_shortname UNIQUE (shortname);

-- component table
-- The acl_id must be unique
ALTER TABLE component
	ADD CONSTRAINT component_unique_acl_id UNIQUE (acl_id);

-- user_account table
-- The email must be unique
ALTER TABLE user_account
	ADD CONSTRAINT user_account_unique_email UNIQUE (email);

-- The login_name must be unique
ALTER TABLE user_account
	ADD CONSTRAINT user_account_unique_login_name UNIQUE (login_name);

-- The nick must be unique
ALTER TABLE user_account
	ADD CONSTRAINT user_account_unique_nick UNIQUE (nick);

-- The acl_id must be unique
ALTER TABLE user_account
	ADD CONSTRAINT user_account_unique_acl_id UNIQUE (acl_id);

-- unit table
-- The name must be unique
ALTER TABLE unit
	ADD CONSTRAINT unit_unique_name UNIQUE (name);

-- The acl_id must be unique
ALTER TABLE unit
	ADD CONSTRAINT unit_unique_acl_id UNIQUE (acl_id);
