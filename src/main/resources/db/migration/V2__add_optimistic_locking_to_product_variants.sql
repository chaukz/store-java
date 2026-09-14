-- V2: add optimistic locking to product_variants.
--
-- Fixes the checkout race condition: two customers buying the last unit
-- in stock at the same moment could previously both pass the stock
-- check and both have their purchase succeed - oversold by one. With a
-- version column, Hibernate adds "AND version = ?" to every UPDATE on
-- this table and bumps the version each time. If two transactions read
-- the same row and both try to update it, only the first to commit
-- succeeds - the second's UPDATE matches zero rows, Hibernate notices,
-- and throws instead of silently doing nothing.

ALTER TABLE product_variants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;