# Interview Preparation Guide

## 1. Data Flow (learn by heart)

```
wtj_2018_04_int_1.xml
        │
        ▼
  XmlValidator          → check that XML is well-formed
        │
        ▼
  XmlToHtmlConverter    → loads article-to-html.xsl
        │                  Saxon calls doc($collectionsUrl)
        │                  → fetches collections.xml (live URL)
        │                  → finds collection_code="wtj"
        │                  → extracts collection_name="World Tax Journal"
        │
        ▼
   article.html         → section outline only, nothing else

wtj_2018_04_int_1.xml
        │
        ▼
  SchematronRunner      → Step 1: transpile.xsl + pubdate-check.sch → check.xsl (in memory)
                          Step 2: check.xsl + XML → report.xml (SVRL)
        │
        ▼
   report.xml           → 2 failed assertions
```

---

## 2. The 5 Most Likely Questions

**"Why Saxon?"**
> XSLT 2.0 requires a processor that supports XPath 2.0 functions such as `tokenize()`, `index-of()`, `xs:integer()` — Java's built-in JAXP processor supports only XSLT 1.0. Saxon HE is open-source and the standard choice for this.

**"Why two steps for Schematron?"**
> The ISO Schematron standard does not define a direct validator — it defines an XSLT transformation. SchXslt2 first "compiles" the `.sch` rules into an executable XSLT (`check.xsl`), which then validates the XML and produces an SVRL report. Those are two separate Saxon invocations.

**"Why is the collection name in an external file instead of hardcoded?"**
> Because the same XSLT stylesheet needs to work for any IBFD journal — only the `collection` attribute changes. The lookup makes the stylesheet generic.

**"Why keep a local copy of collections.xml if you use the live URL?"**
> Fallback — if the live URL is unavailable (offline work, firewall), a single line in `application.properties` switches to the local copy with no code changes required.

**"Why do both Schematron tests FAIL?"**
> The XML is intentionally broken: `<year>2019</year>` but the pubdate attribute says 2018, and the text date says "11 September" but the attribute says "-09-07" (day 7). This is a test for the validation rules — the rule must catch the error.

---

## 3. Walk Through Each File (30 min)

Go through the files in this order:

1. `AppRunner.java` — entry point, see the order of calls
2. `article-to-html.xsl` — understand each template
3. `pubdate-check.sch` — understand the XPath logic for date parsing
4. `XmlToHtmlConverter.java` — how the parameter is passed to the XSLT
5. `SchematronRunner.java` — the two Saxon invocations

---

## 4. Run the Project Once Before the Interview

```powershell
./gradlew bootRun
```

Open `output/article.html` in a browser and `output/report.xml` in an editor.  
You should know **what the output looks like** without looking at the code.

---

## 5. Debug Mode — Only If Asked

If asked to demonstrate debugging, place breakpoints at:

| File | Line | Why |
|------|------|-----|
| `AppRunner.java` | 44 | Start of Step 2 (XSLT transform) |
| `XmlToHtmlConverter.java` | 38 | Where `collectionsUrl` is passed to the XSLT |
| `SchematronRunner.java` | 31 | After transpilation — see that `check.xsl` was generated in memory |

---

## Key Message for the Interview

You don't need to know the Saxon API by heart.  
You need to be able to explain **why** you made each decision:

- Local copy vs. live URL
- Saxon vs. JAXP
- Two steps for Schematron

That is what separates a good developer from a copy-paste developer.
