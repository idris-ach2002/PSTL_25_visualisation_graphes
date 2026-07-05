EMCC ?= emcc
SRC := src/graph_engine.c
OUT_DIR ?= ../frontend/public/wasm
OUT_JS := $(OUT_DIR)/graph-engine.js
EXPORTED_FUNCTIONS := ["_malloc","_free","_ge_init_from_edges","_ge_init_random","_ge_step","_ge_free","_ge_get_node_count","_ge_get_edge_count","_ge_get_live_node_count","_ge_get_iteration","_ge_get_positions_ptr","_ge_get_colors_ptr","_ge_get_edges_ptr","_ge_get_degrees_ptr","_ge_get_weights_ptr","_ge_get_communities_ptr","_ge_get_deleted_ptr","_ge_set_dimensions","_ge_set_force_params","_ge_set_node_position","_ge_delete_node","_ge_restore_node","_ge_run_label_propagation","_ge_run_spatial_kmeans","_ge_reset_layout"]

.PHONY: wasm native clean

wasm:
	mkdir -p $(OUT_DIR)
	$(EMCC) $(SRC) -O3 -flto -msimd128 \
		-s WASM=1 \
		-s MODULARIZE=1 \
		-s EXPORT_ES6=1 \
		-s ENVIRONMENT=web,worker \
		-s ALLOW_MEMORY_GROWTH=1 \
		-s INITIAL_MEMORY=67108864 \
		-s EXPORTED_FUNCTIONS='$(EXPORTED_FUNCTIONS)' \
		-s EXPORTED_RUNTIME_METHODS='["ccall","cwrap"]' \
		-o $(OUT_JS)

native:
	cc -O3 -Wall -Wextra -pedantic -c $(SRC) -o /tmp/graph_engine_check.o

clean:
	rm -f $(OUT_DIR)/graph-engine.js $(OUT_DIR)/graph-engine.wasm /tmp/graph_engine_check.o /tmp/graph_engine_check
