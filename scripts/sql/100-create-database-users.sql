--
-- Create Passkey user.
--

-- You need to execute this as the postgres superuser
--
-- e.g.,
--
-- psql -h localhost -d postgres -U postgres -f 100-create-database-users.sql
--
DO
$do$
    BEGIN
        IF NOT EXISTS(SELECT FROM pg_catalog.pg_roles WHERE rolname = 'passkey') THEN CREATE USER passkey WITH ENCRYPTED PASSWORD 'itsasecret'; END IF;
    END;
$do$;

--
-- END
--
