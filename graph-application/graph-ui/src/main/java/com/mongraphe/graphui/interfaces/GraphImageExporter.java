package com.mongraphe.graphui.interfaces;

import java.io.File;

public interface GraphImageExporter {
    void exportPng(File file, int width, int height);
}