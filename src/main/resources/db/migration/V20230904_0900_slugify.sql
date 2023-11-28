-- Unaccent is used to convert characters like æøå to visually similar ASCI characters.
CREATE EXTENSION IF NOT EXISTS "unaccent";

-- Slugify function
CREATE OR REPLACE FUNCTION slugify(val TEXT)
RETURNS TEXT AS $$
DECLARE
  slug TEXT;
BEGIN
  slug := unaccent(val); -- Unaccent
  slug := lower(slug); -- Lowercase
  slug := regexp_replace(slug, '[''"]+', '', 'gi'); -- Remove quotes
  slug := regexp_replace(slug, '[^a-z0-9\\-_]+', '-', 'gi'); -- Hyphenate
  slug := regexp_replace(regexp_replace(slug, '\-+$', ''), '^\-', ''); -- Trim hyphens
  RETURN slug;
END
$$
LANGUAGE plpgsql
volatile;


-- Trigger function, takes any number of arguments. Those arguments are column names,
-- and the slug is based on values from those columns.
CREATE OR REPLACE FUNCTION update_slug()
RETURNS TRIGGER AS $$
DECLARE
  maxSlugLength INTEGER := 200;
  columnName TEXT;
  slugComponent TEXT;
  baseSlug TEXT;
  slug TEXT DEFAULT '';
  attempts INTEGER DEFAULT 0;
  randomLength INTEGER DEFAULT 4;
  result RECORD;
BEGIN

  -- Don't update slug if it's already set
  IF NEW.slug IS NOT NULL THEN
    RETURN NEW;
  END IF;

  -- Build the slug base from the column names given as arguments
  FOREACH columnName IN ARRAY TG_ARGV LOOP
    EXECUTE 'SELECT ($1).' || quote_ident(columnName) || '::text'
    INTO slugComponent
    USING NEW;

    IF slugComponent IS NOT NULL THEN
      slug := slug || '-' || slugComponent; -- Leading hyphen is removed later
    END IF;
  END LOOP;

  -- If length is 0, don't set slug
  IF length(slug) = 0 THEN
    RAISE 'Slug is empty, not setting slug: %', TG_RELNAME;
    RETURN NEW;
  END IF;

  -- Trim the base to the maximum length
  slug := substring(slug FROM 1 FOR maxSlugLength);

  -- "Slugify"
  slug := slugify(slug);

  -- Keep the base slug, in case we need to add a random suffix
  baseSlug := slug;

  LOOP
    BEGIN
      -- Try to update slug
      EXECUTE format('UPDATE %I SET slug = $1 WHERE _id = $2', TG_RELNAME)
      USING slug, NEW._id;
      EXIT;

    EXCEPTION WHEN unique_violation THEN

      -- Increase the suffix length if we've tried a few times
      IF attempts > 5 THEN
        randomLength := 8;
      END IF;

      -- Try again with a new slug (with a random suffix)
      slug := baseSlug || '-' || SUBSTRING(MD5(RANDOM()::text) FROM 1 FOR randomLength);

      -- Try at most 10 times
      attempts := attempts + 1;
      IF attempts > 10 THEN
        -- No slug!
        EXIT;
      END IF;
    END;
  END LOOP;

  RETURN NEW;
END
$$ LANGUAGE plpgsql;
