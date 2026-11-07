.PHONY: install dev build check test

install:
	corepack pnpm install --frozen-lockfile

dev:
	corepack pnpm dev

build:
	corepack pnpm build

check:
	corepack pnpm check

test:
	corepack pnpm test -- --run
