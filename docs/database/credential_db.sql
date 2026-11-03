--
-- PostgreSQL database dump
--

\restrict b3WUbJ1MZo8C4kNdgwyeBAhNOHd6VqAGOeaeXYASphopZMdNFaNQsNnpgaqRz78

-- Dumped from database version 16.13
-- Dumped by pg_dump version 16.13

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: credentials; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.credentials (
    user_id bigint NOT NULL,
    username character varying(100) NOT NULL,
    password_hash text NOT NULL
);


--
-- Data for Name: credentials; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.credentials (user_id, username, password_hash) FROM stdin;
1	admin	$argon2id$v=19$m=65536,t=3,p=4$ZQyDlXwFk8sDR0iOMZI3DA$Hiz6dsTIoNRkgLH6wwZQYRATlB1bRDDKzrGpPaAkb6Q
10	client.demo	$argon2id$v=19$m=16384,t=2,p=1$R3ux2k1OeEdyzJseq15Ecg$0HjUYnASwvFTcJhsRGE6ZnMIXKt25hTPkgZcV/CIuso
11	client2.demo	$argon2id$v=19$m=16384,t=2,p=1$9wse7otqT9uG5Q2B2a7nqA$jYUaBDtT7UDNsZVreSFZs7oPM1oms5nDugX9hvHiw+A
\.


--
-- Name: credentials credentials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credentials
    ADD CONSTRAINT credentials_pkey PRIMARY KEY (user_id);


--
-- Name: credentials credentials_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credentials
    ADD CONSTRAINT credentials_username_key UNIQUE (username);


--
-- PostgreSQL database dump complete
--

\unrestrict b3WUbJ1MZo8C4kNdgwyeBAhNOHd6VqAGOeaeXYASphopZMdNFaNQsNnpgaqRz78

