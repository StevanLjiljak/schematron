package com.ibfd.schematron.web;

import com.ibfd.schematron.model.FailedAssertion;
import com.ibfd.schematron.model.PipelineResult;
import com.ibfd.schematron.service.PipelineService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class DashboardController {

    private final PipelineService pipelineService;

    public DashboardController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard() {
        try {
            PipelineResult result = pipelineService.run();
            return buildHtml(result);
        } catch (Exception e) {
            return buildErrorHtml(e);
        }
    }

    // ── HTML builder ─────────────────────────────────────────────────────────

    private String buildHtml(PipelineResult r) {
        int failCount = r.failedAssertions().size();
        String step3Class  = failCount == 0 ? "ok"   : "warn";
        String step3Icon   = failCount == 0 ? "✅"    : "⚠️";
        String step3Label  = failCount == 0
                ? "Schematron: All rules pass"
                : "Schematron: " + failCount + " issue" + (failCount == 1 ? "" : "s") + " found";

        String bodyHtml = extractBody(r.articleHtml());
        String schPanel = buildSchematronPanel(r.failedAssertions());

        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>IBFD XML Pipeline Dashboard</title>
<style>
  *{box-sizing:border-box;margin:0;padding:0}
  body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
       background:#f0f2f5;color:#333}

  /* ── Header ─────────────────────────────────────────────── */
  header{background:linear-gradient(135deg,#1565c0,#283593);
         color:#fff;padding:26px 36px}
  header h1{font-size:22px;font-weight:600;margin-bottom:5px;letter-spacing:-.3px}
  header .sub{font-size:12px;opacity:.7;font-family:'Courier New',monospace}

  /* ── Pipeline step bar ───────────────────────────────────── */
  .pipe-bar{display:flex;align-items:stretch;background:#fff;
            border-bottom:1px solid #dde2ea;padding:0 36px;gap:4px;flex-wrap:wrap}
  .step{display:flex;align-items:center;gap:8px;padding:14px 20px;
        font-size:13px;font-weight:500;color:#555;
        border-bottom:3px solid transparent}
  .step.ok  {border-color:#43a047;color:#2e7d32}
  .step.warn{border-color:#fb8c00;color:#bf360c}
  .pipe-arrow{display:flex;align-items:center;color:#ccc;font-size:18px;
              padding:0 2px;user-select:none}

  /* ── Two-column layout ───────────────────────────────────── */
  .grid{display:grid;grid-template-columns:1fr 1fr;
        gap:22px;padding:26px 36px}
  @media(max-width:900px){.grid{grid-template-columns:1fr}}

  /* ── Panel card ──────────────────────────────────────────── */
  .panel{background:#fff;border-radius:10px;
         box-shadow:0 2px 8px rgba(0,0,0,.07);overflow:hidden}
  .panel-hdr{display:flex;align-items:center;justify-content:space-between;
             padding:14px 20px;background:#fafbfc;
             border-bottom:1px solid #e8eaed}
  .panel-hdr h2{font-size:14px;font-weight:600;color:#333}
  .badge{font-size:11px;padding:3px 10px;border-radius:11px;font-weight:600}
  .badge-ok  {background:#e8f5e9;color:#2e7d32}
  .badge-warn{background:#fff3e0;color:#e65100}
  .panel-body{padding:20px 22px;max-height:68vh;overflow-y:auto}

  /* ── Article outline ─────────────────────────────────────── */
  .outline h1{font-size:14px;font-weight:700;color:#1565c0;
              padding:9px 0 7px;border-bottom:1px solid #f0f0f0;margin-bottom:2px}
  .outline h2{font-size:13px;font-weight:500;color:#37474f;
              padding:5px 0 4px 18px}
  .outline h3{font-size:12px;font-weight:400;color:#546e7a;
              padding:3px 0 3px 36px}
  .outline h4,.outline h5,.outline h6{font-size:12px;font-weight:400;
              color:#78909c;padding:2px 0 2px 54px}
  .outline .label{font-weight:700}

  /* ── Assertion card ──────────────────────────────────────── */
  .assertion{border-left:3px solid #ef5350;background:#fff5f5;
             border-radius:0 8px 8px 0;padding:14px 16px;margin-bottom:14px}
  .assertion:last-child{margin-bottom:0}
  .a-top{display:flex;align-items:center;gap:10px;margin-bottom:8px}
  .a-fail{background:#ef5350;color:#fff;font-size:10px;font-weight:700;
          padding:2px 9px;border-radius:10px;letter-spacing:.5px}
  .a-pattern{font-family:'Courier New',monospace;font-size:11px;color:#777}
  .a-location{font-size:11px;color:#999;font-family:'Courier New',monospace;
              margin-bottom:6px}
  .a-test{font-family:'Courier New',monospace;font-size:11px;color:#555;
          background:#f4f4f4;padding:4px 8px;border-radius:4px;
          margin-bottom:8px;display:inline-block}
  .a-msg{font-size:12px;color:#444;line-height:1.65;
         background:#fff;padding:8px 10px;border-radius:5px;
         white-space:pre-wrap}

  /* ── All-pass banner ─────────────────────────────────────── */
  .all-pass{display:flex;align-items:center;gap:10px;
            padding:16px 18px;border-radius:8px;
            background:#e8f5e9;color:#2e7d32;font-weight:500;font-size:14px}
</style>
</head>
<body>

<header>
  <h1>IBFD XML Processing Pipeline</h1>
  <p class="sub">Source: wtj_2018_04_int_1.xml</p>
</header>

<div class="pipe-bar">
  <div class="step ok">✅ Step 1 &mdash; XML Well-Formed</div>
  <div class="pipe-arrow">›</div>
  <div class="step ok">✅ Step 2 &mdash; HTML Generated</div>
  <div class="pipe-arrow">›</div>
  <div class="step %s3class%">%s3icon% Step 3 &mdash; %s3label%</div>
</div>

<div class="grid">

  <div class="panel">
    <div class="panel-hdr">
      <h2>&#128196; Document Outline</h2>
      <span class="badge badge-ok">XSLT 2.0</span>
    </div>
    <div class="panel-body outline">
      %bodyHtml%
    </div>
  </div>

  <div class="panel">
    <div class="panel-hdr">
      <h2>&#128269; Schematron Report</h2>
      <span class="badge %badgeClass%">%badgeLabel%</span>
    </div>
    <div class="panel-body">
      %schPanel%
    </div>
  </div>

</div>
</body>
</html>
"""
                .replace("%s3class%",  step3Class)
                .replace("%s3icon%",   step3Icon)
                .replace("%s3label%",  step3Label)
                .replace("%bodyHtml%", bodyHtml)
                .replace("%badgeClass%", failCount == 0 ? "badge-ok" : "badge-warn")
                .replace("%badgeLabel%", failCount == 0 ? "PASS" : failCount + " FAIL" + (failCount == 1 ? "" : "S"))
                .replace("%schPanel%",   schPanel);
    }

    private String buildSchematronPanel(List<FailedAssertion> assertions) {
        if (assertions.isEmpty()) {
            return "<div class=\"all-pass\">&#10003; All Schematron rules passed.</div>";
        }
        StringBuilder sb = new StringBuilder();
        for (FailedAssertion a : assertions) {
            sb.append("""
<div class="assertion">
  <div class="a-top">
    <span class="a-fail">FAIL</span>
    <span class="a-pattern">%patternId%</span>
  </div>
  <div class="a-location">%location%</div>
  <div class="a-test">%test%</div>
  <div class="a-msg">%message%</div>
</div>
"""
                    .replace("%patternId%", esc(a.patternId()))
                    .replace("%location%",  esc(a.location()))
                    .replace("%test%",      esc(a.test()))
                    .replace("%message%",   esc(a.message())));
        }
        return sb.toString();
    }

    private String extractBody(String html) {
        Matcher m = Pattern.compile("(?s)<body>(.*)</body>").matcher(html);
        return m.find() ? m.group(1).trim() : html;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ── Error fallback ────────────────────────────────────────────────────────

    private String buildErrorHtml(Exception e) {
        return """
<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8">
<title>Pipeline Error</title>
<style>body{font-family:sans-serif;padding:40px;background:#fff5f5}
h1{color:#c62828}pre{background:#fff;border:1px solid #fcc;padding:16px;border-radius:6px;overflow-x:auto}</style>
</head><body>
<h1>&#9888; Pipeline failed to run</h1>
<p style="margin:16px 0;color:#555">""" + esc(e.getClass().getSimpleName() + ": " + e.getMessage()) + """
</p>
<pre>""" + esc(stackTraceOf(e)) + """
</pre>
</body></html>""";
    }

    private String stackTraceOf(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement el : e.getStackTrace()) {
            sb.append(el).append('\n');
            if (sb.length() > 2000) { sb.append("..."); break; }
        }
        return sb.toString();
    }
}
