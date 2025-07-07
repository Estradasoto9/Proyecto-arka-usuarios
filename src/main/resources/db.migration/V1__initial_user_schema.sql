-- V1__initial_user_schema.sql
-- Migración inicial para el esquema del microservicio de Usuarios

CREATE TABLE public.users (
	id uuid DEFAULT gen_random_uuid() NOT NULL,
	"name" varchar(255) NOT NULL,
	phone varchar(20) NULL,
	email varchar(255) NOT NULL,
	"password" varchar(255) NOT NULL,
	active bool DEFAULT true NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	username varchar(50) NOT NULL,
	CONSTRAINT user_email_key UNIQUE (email),
	CONSTRAINT user_pkey PRIMARY KEY (id),
	CONSTRAINT users_username_key UNIQUE (username)
);

CREATE TABLE public."role" (
	id uuid DEFAULT gen_random_uuid() NOT NULL,
	"name" varchar(50) NOT NULL,
	CONSTRAINT role_name_key UNIQUE (name),
	CONSTRAINT role_pkey PRIMARY KEY (id)
);

CREATE TABLE public.user_role (
	id uuid DEFAULT gen_random_uuid() NOT NULL,
	user_id uuid NOT NULL,
	role_id uuid NOT NULL,
	CONSTRAINT user_role_pkey1 PRIMARY KEY (id)
);

-- public.user_role foreign keys

ALTER TABLE public.user_role ADD CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES public."role"(id) ON DELETE CASCADE;
ALTER TABLE public.user_role ADD CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;