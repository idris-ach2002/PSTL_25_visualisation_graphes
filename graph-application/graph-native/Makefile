CC := gcc
CSTD := -std=c11
UNAME_S := $(shell uname -s)

# Extension selon OS (pour simplifier le chargement de la librairie en Java)
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
    # Windows : on prend JAVA_HOME s'il est défini
    ifndef JAVA_HOME
        $(error "Please set JAVA_HOME environment variable on Windows")
    endif
else
    # Linux / macOS : on peut détecter java dans le PATH
    ifndef JAVA_HOME
        JAVA_BIN := $(shell which java)
        ifeq ($(JAVA_BIN),)
            $(error "java not found in PATH, please set JAVA_HOME")
        endif
        JAVA_HOME := $(shell readlink -f $(JAVA_BIN) | sed "s:/bin/java::")
    endif
endif

JNI_INC := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(JNI_OS)

JNI_INC := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(JNI_OS)

SRC_DIR := c
OUT_DIR := out

TEST_SRC := c/tests/test_engine.c c/tests/test_utils.c
TEST_BIN := $(OUT_DIR)/test_engine

SRC := \
    $(wildcard $(SRC_DIR)/c_graph/*.c) \
    $(wildcard $(SRC_DIR)/concurrent/*.c) \
    $(wildcard $(SRC_DIR)/debug/*.c) \
    $(wildcard $(SRC_DIR)/pretraitement/*.c) \
    $(SRC_DIR)/forceatlasV4_CSV.c

test: $(TEST_BIN)
	@echo "Running tests"
	$(TEST_BIN) c/tests/graphs/triangle.dot

OBJ := $(patsubst $(SRC_DIR)/%.c,$(OUT_DIR)/%.o,$(SRC))

LIB := $(OUT_DIR)/libnative.$(EXT)

CFLAGS := -Wall -Wextra -O2 -fPIC $(CSTD) $(COMPILER_FLAGS) $(JNI_INC)

all: $(LIB) $(TEST_BIN)

$(LIB): $(OBJ)
	@echo "🔗 Linking $@"
	@mkdir -p $(dir $@)
	$(CC) -shared $^ -o $@

$(OUT_DIR)/%.o: $(SRC_DIR)/%.c
	@echo "⚙️  $<"
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) -c $< -o $@

$(TEST_BIN): $(OBJ) $(TEST_SRC)
	@echo "🧪 Building test binary"
	$(CC) $(CFLAGS) $^ -lm -o $@

clean:
	rm -rf out

.PHONY: all clean