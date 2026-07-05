.PHONY: prod dev wasm clean

prod:
	docker compose up --build graph-web

dev:
	docker compose --profile dev up --build graph-web-dev

wasm:
	make -C wasm-engine wasm

clean:
	rm -rf frontend/dist frontend/src/wasm/graph-engine.js frontend/src/wasm/graph-engine.wasm
