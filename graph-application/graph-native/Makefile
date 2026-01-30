# =========================
# Configuration générale
# =========================

CC      := gcc
UNAME_S := $(shell uname -s)

# Détection OS (ici Linux only, simple et robuste)
ifeq ($(UNAME_S),Linux)
    OS      := linux
    EXT     := so
    JNI_OS  := linux
else ifeq ($(UNAME_S),Darwin)
    OS      := macos
    EXT     := dylib
    JNI_OS  := darwin
else
    OS      := windows
    EXT     := dll
    JNI_OS  := win32
endif

# JAVA_HOME obligatoire pour JNI
JAVA_HOME ?= $(shell readlink -f /usr/bin/java | sed "s:/bin/java::")

JNI_INC := \
    -I$(JAVA_HOME)/include \
    -I$(JAVA_HOME)/include/$(JNI_OS)

# Dossiers
SRC_DIR := c
OUT_DIR := out/$(OS)

# Sources C (récursif)
SRC := \
    $(wildcard $(SRC_DIR)/c_graph/*.c) \
    $(wildcard $(SRC_DIR)/concurrent/*.c) \
    $(wildcard $(SRC_DIR)/debug/*.c) \
    $(wildcard $(SRC_DIR)/pretraitement/*.c) \
    $(SRC_DIR)/forceatlasV4_CSV.c

OBJ := $(patsubst $(SRC_DIR)/%.c,$(OUT_DIR)/%.o,$(SRC))

LIB := $(OUT_DIR)/libnative.$(EXT)

CFLAGS := -Wall -Wextra -O2 -fPIC $(JNI_INC)

# =========================
# Règles
# =========================

all: $(LIB)

$(LIB): $(OBJ)
	@echo "🔗 Linking JNI library: $@"
	@mkdir -p $(dir $@)
	$(CC) -shared $^ -o $@

$(OUT_DIR)/%.o: $(SRC_DIR)/%.c
	@echo "⚙️  Compiling $<"
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	rm -rf out

.PHONY: all clean
