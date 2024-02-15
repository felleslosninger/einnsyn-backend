ALTER TABLE møtesaksregistrering ALTER COLUMN møtesaksår TYPE int USING (møtesaksår::integer);
