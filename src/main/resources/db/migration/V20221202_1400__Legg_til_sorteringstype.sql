ALTER TABLE møtesaksregistrering
  ADD COLUMN IF NOT EXISTS sorteringstype text;
ALTER TABLE journalpost
    ADD COLUMN IF NOT EXISTS sorteringstype text;