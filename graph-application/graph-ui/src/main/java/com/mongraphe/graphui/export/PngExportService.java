package com.mongraphe.graphui.export;

public class PngExportService implements ExportService {

    private final GraphExport exporter;

    public PngExportService(GraphExport exporter) {
        this.exporter = exporter;
    }

    @Override
    public void export(String path) {
        exporter.scheduleExport(path);
    }
}