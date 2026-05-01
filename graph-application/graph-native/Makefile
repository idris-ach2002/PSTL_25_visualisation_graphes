CC := gcc
CSTD := -std=c11
UNAME_S := $(shell uname -s)

ifeq ($(UNAME_S),Linux)
    EXT := so
    JNI_OS := linux
    SHARED_FLAGS := -shared
else ifeq ($(UNAME_S),Darwin)
    EXT := dylib
    JNI_OS := darwin
    SHARED_FLAGS := -dynamiclib
else
    EXT := dll
    JNI_OS := win32
    SHARED_FLAGS := -shared
endif

ifeq ($(OS),Windows_NT)
    ifndef JAVA_HOME
        $(error "Please set JAVA_HOME environment variable on Windows")
    endif
else
    ifndef JAVA_HOME
        JAVA_BIN := $(shell which java)
        ifeq ($(JAVA_BIN),)
            $(error "java not found in PATH, please set JAVA_HOME")
        endif
        JAVA_HOME := $(shell readlink -f $(JAVA_BIN) | sed "s:/bin/java::")
    endif
endif

JNI_INC := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(JNI_OS)

SRC_DIR := c
OUT_DIR := out
OUT_OS_DIR := $(OUT_DIR)/$(JNI_OS)

SRC := \
    $(wildcard $(SRC_DIR)/c_graph/*.c) \
    $(wildcard $(SRC_DIR)/concurrent/*.c) \
    $(wildcard $(SRC_DIR)/debug/*.c) \
    $(wildcard $(SRC_DIR)/pretraitement/*.c) \
    $(SRC_DIR)/forceatlasV4_CSV.c

OBJ := $(patsubst $(SRC_DIR)/%.c,$(OUT_DIR)/%.o,$(SRC))
DEP := $(OBJ:.o=.d)

LIB := $(OUT_DIR)/libnative.$(EXT)
LIB_OS := $(OUT_OS_DIR)/libnative.$(EXT)

# ----------------------------------------------------------------------
# Configuration compilation
# ----------------------------------------------------------------------
# -MMD -MP génère automatiquement les dépendances vers les headers.
# Donc si global.h, graph.h, communities.h, cluster.h, etc. changent,
# les .o concernés sont recompilés.
# Le niveau d'optimisation par défaut est -O3.
# ----------------------------------------------------------------------

OPT_FLAGS ?= -O3 -march=native
WARN_FLAGS := -Wall -Wextra
DEP_FLAGS := -MMD -MP
PIC_FLAGS := -fPIC
THREAD_FLAGS := -pthread

BASE_CFLAGS := \
    $(WARN_FLAGS) \
    $(OPT_FLAGS) \
    $(PIC_FLAGS) \
    $(CSTD) \
    $(DEP_FLAGS) \
    $(THREAD_FLAGS) \
    $(JNI_INC)

BASE_LDFLAGS := -lm $(THREAD_FLAGS)

CFLAGS := $(BASE_CFLAGS) $(COMPILER_FLAGS) $(EXTRA_CFLAGS)
LDFLAGS := $(BASE_LDFLAGS) $(EXTRA_LDFLAGS)

.PHONY: all clean rebuild debug asan print-config

all: $(LIB) $(LIB_OS)

$(LIB): $(OBJ)
	@echo "🔗 Linking $@"
	@mkdir -p $(dir $@)
	$(CC) $(SHARED_FLAGS) $^ -o $@ $(LDFLAGS)

$(LIB_OS): $(LIB)
	@echo "📦 Copying $@"
	@mkdir -p $(dir $@)
	cp $(LIB) $(LIB_OS)

# Chaque .o dépend du .c, du Makefile, et automatiquement des .h via les .d.
$(OUT_DIR)/%.o: $(SRC_DIR)/%.c Makefile
	@echo "⚙️  $<"
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) -c $< -o $@

# Inclusion des dépendances auto-générées.
-include $(DEP)

clean:
	rm -rf $(OUT_DIR)

# Rebuild complet
rebuild: clean all

print-config:
	@echo "CC          = $(CC)"
	@echo "JAVA_HOME   = $(JAVA_HOME)"
	@echo "JNI_OS      = $(JNI_OS)"
	@echo "EXT         = $(EXT)"
	@echo "SRC         = $(SRC)"
	@echo "OBJ         = $(OBJ)"
	@echo "DEP         = $(DEP)"
	@echo "CFLAGS      = $(CFLAGS)"
	@echo "LDFLAGS     = $(LDFLAGS)"
	@echo "LIB         = $(LIB)"
	@echo "LIB_OS      = $(LIB_OS)"