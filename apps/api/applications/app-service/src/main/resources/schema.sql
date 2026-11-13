CREATE TABLE IF NOT EXISTS "franchise" (
	"id" bigserial NOT NULL,
	"name" text NOT NULL,
	PRIMARY KEY ("id")
);

CREATE TABLE IF NOT EXISTS "branch" (
	"id" bigserial NOT NULL,
	"name" text NOT NULL,
	"franchise_id" bigint NOT NULL REFERENCES "franchise"("id"),
	PRIMARY KEY ("id")
);

CREATE TABLE IF NOT EXISTS "product" (
	"id" bigserial NOT NULL,
	"name" varchar(255) NOT NULL,
	"stock" integer NOT NULL,
	"branch_id" bigint NOT NULL REFERENCES "branch"("id"),
	PRIMARY KEY ("id")
);
