-- Arkiv
DROP TRIGGER IF EXISTS enrich_legacy_arkiv_trigger ON arkiv;
DROP TRIGGER IF EXISTS enrich_legacy_arkiv_journalenhet_trigger ON arkiv;
DROP FUNCTION IF EXISTS enrich_legacy_arkiv();

-- Arkivdel
DROP TRIGGER IF EXISTS enrich_legacy_arkivdel_trigger ON arkivdel;
DROP TRIGGER IF EXISTS enrich_legacy_arkivdel_journalenhet_trigger ON arkivdel;
DROP FUNCTION IF EXISTS enrich_legacy_arkivdel();

-- Dokumentbeskrivelse / Dokumentobjekt
DROP TRIGGER IF EXISTS enrich_legacy_dokumentbeskrivelse_trigger ON dokumentbeskrivelse;
DROP TRIGGER IF EXISTS enrich_legacy_dokumentbeskrivelse_journalenhet_trigger ON dokumentbeskrivelse;
DROP TRIGGER IF EXISTS enrich_legacy_dokumentobjekt_journalenhet_trigger ON dokumentobjekt;
DROP FUNCTION IF EXISTS enrich_legacy_dokumentbeskrivelse();

-- Klasse
DROP TRIGGER IF EXISTS enrich_legacy_klasse_trigger ON klasse;
DROP TRIGGER IF EXISTS enrich_legacy_klasse_journalenhet_trigger ON klasse;
DROP FUNCTION IF EXISTS enrich_legacy_klasse();

-- Korrespondansepart
DROP TRIGGER IF EXISTS enrich_legacy_korrespondansepart_trigger ON korrespondansepart;
DROP TRIGGER IF EXISTS enrich_legacy_korrespondansepart_journalenhet_trigger ON korrespondansepart;
DROP FUNCTION IF EXISTS enrich_legacy_korrespondansepart();

-- Journalenhet (shared trigger function used across multiple entities)
DROP TRIGGER IF EXISTS enrich_legacy_journalpost_journalenhet_trigger ON journalpost;
DROP TRIGGER IF EXISTS enrich_legacy_moetedokumentregistrering_journalenhet_trigger ON møtedokumentregistrering;
DROP TRIGGER IF EXISTS enrich_legacy_moetemappe_journalenhet_trigger ON møtemappe;
DROP TRIGGER IF EXISTS enrich_legacy_moetesaksbeskrivelse_journalenhet_trigger ON moetesaksbeskrivelse;
DROP TRIGGER IF EXISTS enrich_legacy_moetesaksregistrering_journalenhet_trigger ON møtesaksregistrering;
DROP TRIGGER IF EXISTS enrich_legacy_saksmappe_journalenhet_trigger ON saksmappe;
DROP TRIGGER IF EXISTS enrich_legacy_skjerming_journalenhet_trigger ON skjerming;
DROP FUNCTION IF EXISTS enrich_legacy_journalenhet();

-- Lagret søk / sak
DROP TABLE IF EXISTS lagret_sak_treff;
DROP TABLE IF EXISTS lagret_sok_treff;
DROP FUNCTION IF EXISTS check_daily_hits_on_lagret_sak_id();
DROP FUNCTION IF EXISTS check_daily_hits_on_lagret_sok_id();
