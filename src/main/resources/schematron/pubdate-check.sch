<?xml version="1.0" encoding="UTF-8"?>
<!--
  Schematron rules for publication-date consistency in IBFD journal articles.

  Checks (both are expected to FAIL on wtj_2018_04_int_1.xml because the file
  contains intentional errors):

  1. The pubdate attribute (YYYY-MM-DD) must represent the same calendar date
     as the human-readable text inside the <pubdate> element (D Month YYYY).
     File has: pubdate="2018-09-07"  vs  text "11 September 2018"  → FAIL (7 ≠ 11)

  2. The <year> element must match the year part of the pubdate attribute.
     File has: <year>2019</year>  vs  pubdate year 2018  → FAIL (2019 ≠ 2018)
-->
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            queryBinding="xslt2">

  <sch:title>Publication Date Consistency Checks</sch:title>

  <sch:ns prefix="xs" uri="http://www.w3.org/2001/XMLSchema"/>

  <sch:pattern id="pubdate-text-vs-attribute">

    <!--
      Rule 1: verify that the YYYY-MM-DD attribute value encodes the same date
      as the "D Month YYYY" text content of <pubdate>.

      Strategy: tokenise the text content on whitespace → [day, monthName, year],
      then convert monthName to a numeric index and compare component by component
      with the corresponding substrings of the @pubdate attribute.
      No format-number() needed – we compare integers directly.
    -->
    <sch:rule context="pubdate[@pubdate]">

      <sch:let name="attr"        value="@pubdate"/>
      <sch:let name="attr-year"   value="substring($attr, 1, 4)"/>
      <sch:let name="attr-month"  value="xs:integer(substring($attr, 6, 2))"/>
      <sch:let name="attr-day"    value="xs:integer(substring($attr, 9, 2))"/>

      <sch:let name="month-names" value="('January','February','March','April','May','June',
                                          'July','August','September','October','November','December')"/>
      <sch:let name="parts"       value="tokenize(normalize-space(.), '\s+')"/>
      <sch:let name="text-day"    value="xs:integer($parts[1])"/>
      <sch:let name="text-month"  value="xs:integer(index-of($month-names, $parts[2])[1])"/>
      <sch:let name="text-year"   value="$parts[3]"/>

      <sch:assert test="$attr-day = $text-day and $attr-month = $text-month and $attr-year = $text-year">
        [FAIL] pubdate attribute "<sch:value-of select="$attr"/>" does not match the text
        content "<sch:value-of select="."/>".
        Attribute encodes: <sch:value-of select="$attr-year"/>-<sch:value-of
            select="$attr-month"/>-<sch:value-of select="$attr-day"/>;
        text encodes: <sch:value-of select="$text-year"/>-<sch:value-of
            select="$text-month"/>-<sch:value-of select="$text-day"/>.
      </sch:assert>

    </sch:rule>

  </sch:pattern>

  <sch:pattern id="year-vs-pubdate-attribute">

    <!--
      Rule 2: verify that <year> matches the year component of the nearest
      sibling <pubdate pubdate="..."> attribute (both live inside <chaphead>).
    -->
    <sch:rule context="year">

      <sch:let name="pubdate-attr-year" value="substring(../pubdate/@pubdate, 1, 4)"/>

      <sch:assert test=". = $pubdate-attr-year">
        [FAIL] The &lt;year&gt; element value "<sch:value-of select="."/>" does not match
        the year in the pubdate attribute
        "<sch:value-of select="../pubdate/@pubdate"/>" (year = <sch:value-of select="$pubdate-attr-year"/>).
      </sch:assert>

    </sch:rule>

  </sch:pattern>

</sch:schema>
