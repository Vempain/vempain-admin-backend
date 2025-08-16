-- Create site_file table (id, file_name, file_path, mime_type, size, creator, created, updater, updated)
CREATE TABLE IF NOT EXISTS site_file (
    id          BIGSERIAL PRIMARY KEY,
    file_name   VARCHAR(255)  NOT NULL,
    file_path   VARCHAR(2048) NOT NULL,
    mime_type   VARCHAR(255)  NOT NULL,
    size        BIGINT        NOT NULL,
    creator     VARCHAR(255),
    created     TIMESTAMPTZ   DEFAULT now(),
    updater     VARCHAR(255),
    updated     TIMESTAMPTZ
);

-- Unique index to prevent duplicates and speed up lookup by path+name
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = current_schema()
          AND indexname = 'ux_site_file_path_name'
    ) THEN
        CREATE UNIQUE INDEX ux_site_file_path_name ON site_file(file_path, file_name);
    END IF;
END$$;

-- Update gallery_file: replace file_common_id with site_file_id

-- Drop possible legacy constraints referencing file_common_id
ALTER TABLE gallery_file DROP CONSTRAINT IF EXISTS fk_gallery_file_file_common_id;
ALTER TABLE gallery_file DROP CONSTRAINT IF EXISTS gallery_file_file_common_id_fkey;

-- If legacy column exists, rename it to preserve data
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'gallery_file' AND column_name = 'file_common_id'
    ) THEN
        ALTER TABLE gallery_file RENAME COLUMN file_common_id TO site_file_id;
    END IF;
END$$;

-- Ensure site_file_id column exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'gallery_file' AND column_name = 'site_file_id'
    ) THEN
        ALTER TABLE gallery_file ADD COLUMN site_file_id BIGINT;
    END IF;
END$$;

-- Index for faster joins/filtering
CREATE INDEX IF NOT EXISTS idx_gallery_file_site_file_id ON gallery_file(site_file_id);

-- Add FK to site_file(id) if not already present
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
         AND tc.table_schema = kcu.table_schema
        WHERE tc.table_name = 'gallery_file'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND tc.constraint_name = 'fk_gallery_file_site_file'
    ) THEN
        ALTER TABLE gallery_file
            ADD CONSTRAINT fk_gallery_file_site_file
            FOREIGN KEY (site_file_id) REFERENCES site_file(id);
    END IF;
END$$;

