CREATE TABLE IF NOT EXISTS "franchise" (
	"id" bigserial NOT NULL,
	"name" text NOT NULL,
	PRIMARY KEY ("id")
);

CREATE TABLE IF NOT EXISTS "branch" (
	"id" bigserial NOT NULL,
	"name" text NOT NULL,
	"franchise_id" bigint NOT NULL,
	PRIMARY KEY ("id")
);

CREATE TABLE IF NOT EXISTS "product" (
	"id" bigserial NOT NULL,
	"name" varchar(255) NOT NULL,
	"stock" integer NOT NULL,
	"branch_id" bigint NOT NULL,
	PRIMARY KEY ("id")
);

ALTER TABLE "branch" ADD CONSTRAINT "branch_fk2" FOREIGN KEY ("franchise_id") REFERENCES "franchise"("id");
ALTER TABLE "product" ADD CONSTRAINT "product_fk2" FOREIGN KEY ("branch_id") REFERENCES "branch"("id");