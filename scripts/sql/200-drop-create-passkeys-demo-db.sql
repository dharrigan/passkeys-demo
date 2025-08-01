--
-- Let's Go Go Go!
--

--
-- You need to execute this as the postgres superuser
--
-- i.e.,
--
-- psql -h localhost -d postgres -U postgres -f 200-drop-create-passkeys-demo-db.sql
--
\c postgres;
DROP DATABASE IF EXISTS passkeys_demo;
CREATE DATABASE passkeys_demo OWNER passkey;
\c passkeys_demo;

--
-- passkey permissions
--
GRANT CONNECT ON DATABASE passkeys_demo TO passkey;
CREATE SCHEMA IF NOT EXISTS passkey AUTHORIZATION passkey;

--
-- Install pgcrypto extension into the passkey schema
--
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA passkey;

--
-- Remove public permissions for ALL users (good practice)
--
REVOKE ALL ON SCHEMA public FROM PUBLIC;

--
-- All done
--
\c postgres;

--
-- END
--
