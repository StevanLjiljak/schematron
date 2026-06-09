# IBFD Content Engineer Test – Java/XML Exercise

Spring Boot 4.0.6 / Java 21 project implementing the two exercises from the IBFD Content Engineer technical test:

1. **Java/XSLT** – Convert a journal article XML file to an HTML document outline
2. **Schematron** – Validate publication-date consistency rules against the same XML

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 21+ |
| Gradle (wrapper included) | 9.5.1 |

No manual dependency downloads are needed — everything is pulled from Maven Central by Gradle.  
SchXslt2 `v1.10.3` is already bundled at `src/main/resources/schxslt2/transpile.xsl`.

---

## Project Structure

```
src/main/
├── java/com/ibfd/schematron/
│   ├── SchematronApplication.java      # Spring Boot entry point
│   ├── config/
│   │   └── SaxonConfig.java            # Saxon HE Processor Spring bean
│   ├── model/
│   │   ├── FailedAssertion.java        # Record: one SVRL failed-assert
│   │   └── PipelineResult.java         # Record: full pipeline output (HTML + SVRL + assertions)
│   ├── service/
│   │   ├── XmlValidator.java           # XML well-formedness check (Saxon)
│   │   ├── XmlToHtmlConverter.java     # XSLT transformation service
│   │   ├── SchematronRunner.java       # Two-step Schematron pipeline
│   │   ├── SvrlParser.java             # Parses SVRL XML → List<FailedAssertion>
│   │   └── PipelineService.java        # Orchestrates all steps, caches result
│   ├── web/
│   │   └── DashboardController.java    # GET / → HTML dashboard
│   └── runner/
│       └── AppRunner.java              # Runs pipeline on startup, writes output files
└── resources/
    ├── xml/
    │   └── wtj_2018_04_int_1.xml       # Source journal article (input)
    ├── config/
    │   └── collections.xml             # Local copy of IBFD collections lookup
    ├── xslt/
    │   └── article-to-html.xsl         # XSLT 2.0 stylesheet (XML → HTML)
    ├── schematron/
    │   └── pubdate-check.sch           # ISO Schematron rules
    └── schxslt2/
        └── transpile.xsl               # SchXslt2 v1.10.3 transpiler

src/test/java/com/ibfd/schematron/
├── SchematronApplicationTests.java     # Spring context load test
└── service/
    ├── XmlValidatorTest.java           # 4 tests – well-formed / malformed XML
    ├── XmlToHtmlConverterTest.java     # 14 tests – headings, inline markup, body exclusion
    └── SchematronRunnerTest.java       # 6 tests – assertion count, rule IDs, SVRL structure
```

---

## How to Run

### Using the Gradle wrapper (recommended)

```bash
# Windows
gradlew.bat bootRun

# Linux / macOS / Git Bash
./gradlew bootRun
```

The application runs all three steps on startup, writes the output files, then keeps the embedded Tomcat server running. Open **http://localhost:8080** in a browser to see the interactive dashboard.

### Build a runnable JAR

```bash
./gradlew bootJar
java -jar build/libs/schematron-0.0.1-SNAPSHOT.jar
```

---

## What the Application Does

On every run, three steps execute in sequence:

### Step 1 – XML Validation

Checks that `wtj_2018_04_int_1.xml` is well-formed XML using Saxon's document builder.

```
=== Step 1: Validating XML ===
    XML is well-formed.
```

### Step 2 – XML → HTML (XSLT Part 1)

Transforms the journal article XML into an HTML document outline using `article-to-html.xsl`.

**Output:** `output/article.html`

Rules applied by the stylesheet:

| XML element | HTML output |
|-------------|-------------|
| `chaphead/title` + `collections.xml` lookup | `<title>` in `<head>` |
| `<section label="1."><title>Intro</title>` | `<h1><span class="label">1. </span><span class="title">Intro</span></h1>` |
| Nested `<section>` (1 ancestor) | `<h2>` |
| Doubly-nested `<section>` (2 ancestors) | `<h3>` |
| `<sub>` inside `<title>` | `<sub>` (preserved) |
| `<emph type="i">` inside `<title>` | `<i>` (italicised) |
| `<footnote>` inside `<title>` | suppressed entirely |
| All body text, paragraphs, tables, etc. | not included |

The collection name (`World Tax Journal`) is looked up by reading the
`collection="wtj"` attribute from the root element and calling Saxon's `doc()`
function against the configured collections URL (see [Configuration](#configuration) below).

### Step 3 – Schematron Validation (Schematron Part 2)

Runs `pubdate-check.sch` against the article XML in two steps (matching the
Saxon CLI commands from the task specification):

1. **Transpile** — `transpile.xsl` (SchXslt2) converts the `.sch` file into an
   executable XSLT stylesheet (`check.xsl`, held in memory)
2. **Validate** — `check.xsl` is applied to the article XML to produce an
   SVRL report

**Output:** `output/report.xml`

#### Schematron rules

**Rule 1** – `pubdate` attribute vs. element text  
The `pubdate` attribute (`YYYY-MM-DD`) must represent the same calendar date as
the human-readable text inside `<pubdate>` (`D Month YYYY`).

**Rule 2** – `<year>` vs. `pubdate` attribute year  
The `<year>` element must match the year component of the `pubdate` attribute.

#### Expected validation results

The source XML contains **two intentional errors**; both Schematron rules should **FAIL**:

```xml
<year>2019</year>
<pubdate pubdate="2018-09-07">11 September 2018</pubdate>
```

| Check | Values | Result |
|-------|--------|--------|
| `<year>` matches pubdate year | `2019` vs `2018` | **FAIL** |
| Attribute date matches text date | `2018-09-07` (day 7) vs `11 September 2018` (day 11) | **FAIL** |

The SVRL report (`report.xml`) will contain two `<svrl:failed-assert>` elements
with descriptive messages identifying each discrepancy.

---

## Configuration

All settings live in `src/main/resources/application.properties`.

### collections.url

Controls where the XSLT looks up the journal collection name.

| Value | When to use |
|-------|-------------|
| `https://dtd.ibfd.org/dtd/config/collections.xml` | **Default** — fetches the live IBFD file at transform time |
| `classpath:config/collections.xml` | Offline / no internet — uses the bundled local copy |

```properties
# application.properties

# Live URL (default):
collections.url=https://dtd.ibfd.org/dtd/config/collections.xml

# Local fallback (uncomment to use):
# collections.url=classpath:config/collections.xml
```

Saxon's `doc()` function resolves both `https://` and `classpath:` URIs transparently —
no changes to the XSLT are needed when switching between the two options.

---

## Web Dashboard

After startup, navigate to **http://localhost:8080** to see:

- **Pipeline bar** – three step indicators (✅ XML Valid → ✅ HTML Generated → ⚠️/✅ Schematron)
- **Document Outline panel** – the full article heading hierarchy rendered from the XSLT output
- **Schematron Report panel** – each failed assertion displayed as a card with rule name, XPath location, failing test expression, and the descriptive message

---

## Running the Tests

```bash
./gradlew test
```

25 unit tests across three test classes — no Spring context or network connection required:

| Test class | Tests | What is verified |
|------------|------:|-----------------|
| `XmlValidatorTest` | 4 | Well-formed XML passes; malformed / empty XML throws `IllegalArgumentException` |
| `XmlToHtmlConverterTest` | 14 | `<title>` content, `h1`/`h2`/`h3` depth, `<sub>`/`<i>` inline markup, footnote suppression, body text exclusion |
| `SchematronRunnerTest` | 6 | Exactly 2 `failed-assert` elements; correct `patternId` for each rule; valid SVRL namespace |
| `SchematronApplicationTests` | 1 | Spring context wires correctly (Saxon bean, all services) |

---

## Output Files

| File | Description |
|------|-------------|
| `output/article.html` | HTML document outline generated by XSLT |
| `output/report.xml` | SVRL Schematron validation report |

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `org.springframework.boot:spring-boot-starter` | Application container / DI |
| `org.springframework.boot:spring-boot-starter-web` | Embedded Tomcat + Spring MVC (dashboard) |
| `net.sf.saxon:Saxon-HE:12.5` | XSLT 2.0 processor (also used for Schematron) |
| `schxslt2/transpile.xsl` (bundled) | SchXslt2 v1.10.3 — ISO Schematron transpiler |

---

## Equivalent Saxon CLI Commands (Task Reference)

The Schematron pipeline implemented in `SchematronRunner.java` is the programmatic
equivalent of these two commands from the task specification:

```bash
# Step 1 – transpile Schematron → XSLT
java -jar saxon-he-12.9.jar \
     -xsl:schxslt2/transpile.xsl \
     -s:src/main/resources/schematron/pubdate-check.sch \
     -o:check.xsl

# Step 2 – validate XML, produce SVRL report
java -jar saxon-he-12.9.jar \
     -xsl:check.xsl \
     -s:src/main/resources/xml/wtj_2018_04_int_1.xml \
     -o:output/report.xml
```
