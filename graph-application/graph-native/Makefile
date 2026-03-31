CC := gcc
CSTD := -std=c11
UNAME_S := $(shell uname -s)

ifeq ($(UNAME_S),Linux)
    EXT := so
    JNI_OS := linux
else ifeq ($(UNAME_S),Darwin)
    EXT := dylib
    JNI_OS := darwin
else
    EXT := dll
    JNI_OS := win32
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
LIB := $(OUT_DIR)/libnative.$(EXT)
LIB_OS := $(OUT_OS_DIR)/libnative.$(EXT)

CFLAGS := -Wall -Wextra -O2 -fPIC -O3 -march=native $(CSTD) $(COMPILER_FLAGS) $(JNI_INC)
LDFLAGS := -lm

.PHONY: all clean
all: $(LIB) $(LIB_OS)

$(LIB): $(OBJ)
	@echo "🔗 Linking $@"
	@mkdir -p $(dir $@)
	$(CC) -shared $^ -o $@ $(LDFLAGS)

$(LIB_OS): $(LIB)
	@mkdir -p $(dir $@)
	cp $(LIB) $(LIB_OS)

$(OUT_DIR)/%.o: $(SRC_DIR)/%.c
	@echo "⚙️  $<"
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	rm -rf out
