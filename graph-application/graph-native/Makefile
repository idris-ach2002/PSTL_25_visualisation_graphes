CC := gcc
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

SRC := \
    $(wildcard $(SRC_DIR)/c_graph/*.c) \
    $(wildcard $(SRC_DIR)/concurrent/*.c) \
    $(wildcard $(SRC_DIR)/debug/*.c) \
    $(wildcard $(SRC_DIR)/pretraitement/*.c) \
    $(SRC_DIR)/forceatlasV4_CSV.c

OBJ := $(patsubst $(SRC_DIR)/%.c,$(OUT_DIR)/%.o,$(SRC))

LIB := $(OUT_DIR)/libnative.$(EXT)

CFLAGS := -Wall -Wextra -O2 -fPIC $(JNI_INC)

all: $(LIB)

$(LIB): $(OBJ)
	@echo "🔗 Linking $@"
	@mkdir -p $(dir $@)
	$(CC) -shared $^ -o $@

$(OUT_DIR)/%.o: $(SRC_DIR)/%.c
	@echo "⚙️  $<"
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	rm -rf out

.PHONY: all clean