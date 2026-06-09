package com.ibfd.schematron.web;

import com.ibfd.schematron.model.PipelineResult;
import com.ibfd.schematron.service.PipelineService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class DashboardController {

    private final PipelineService pipelineService;

    public DashboardController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        try {
            PipelineResult result = pipelineService.run();
            int failCount = result.failedAssertions().size();

            model.addAttribute("bodyHtml",     extractBody(result.articleHtml()));
            model.addAttribute("assertions",   result.failedAssertions());
            model.addAttribute("failCount",    failCount);
            model.addAttribute("step3Class",   failCount == 0 ? "ok" : "warn");
            model.addAttribute("step3Icon",    failCount == 0 ? "✅" : "⚠️");
            model.addAttribute("step3Label",   failCount == 0
                    ? "Schematron: All rules pass"
                    : "Schematron: " + failCount + " issue" + (failCount == 1 ? "" : "s") + " found");
            model.addAttribute("schBadgeClass", failCount == 0 ? "badge-ok" : "badge-warn");
            model.addAttribute("schBadgeLabel", failCount == 0
                    ? "PASS"
                    : failCount + " FAIL" + (failCount == 1 ? "" : "S"));
        } catch (Exception e) {
            model.addAttribute("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return "dashboard";
    }

    private String extractBody(String html) {
        Matcher m = Pattern.compile("(?s)<body>(.*)</body>").matcher(html);
        return m.find() ? m.group(1).trim() : html;
    }
}
