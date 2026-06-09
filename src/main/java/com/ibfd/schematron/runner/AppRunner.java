package com.ibfd.schematron.runner;

import com.ibfd.schematron.model.PipelineResult;
import com.ibfd.schematron.service.PipelineService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Runs the full pipeline on startup, writes output files to output/, then keeps
 * the embedded web server alive so the dashboard is reachable at http://localhost:8080
 */
@Component
public class AppRunner implements ApplicationRunner {

    private final PipelineService pipelineService;

    public AppRunner(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path outputDir = Path.of("output");
        outputDir.toFile().mkdirs();

        System.out.println("=== Step 1: Validating XML ===");
        System.out.println("=== Step 2: Transforming XML to HTML ===");
        System.out.println("=== Step 3: Running Schematron validation ===");

        PipelineResult result = pipelineService.run();

        // Write article.html
        File htmlFile = outputDir.resolve("article.html").toFile();
        try (FileOutputStream out = new FileOutputStream(htmlFile)) {
            out.write(result.articleHtml().getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("    HTML  → " + htmlFile.getAbsolutePath());

        // Write report.xml
        File reportFile = outputDir.resolve("report.xml").toFile();
        try (FileOutputStream out = new FileOutputStream(reportFile)) {
            out.write(result.svrlReport().getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("    SVRL  → " + reportFile.getAbsolutePath());
        System.out.println("    Failed assertions: " + result.failedAssertions().size());
        System.out.println();
        System.out.println("    Dashboard: http://localhost:8080");
    }
}
