<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs">

  <xsl:output method="html" encoding="UTF-8" indent="yes"
              doctype-public="-//W3C//DTD HTML 4.01//EN"
              doctype-system="http://www.w3.org/TR/html4/strict.dtd"/>

  <!--
    collectionsUrl: absolute URI to collections.xml (passed from Java so Saxon
    can resolve it as a local file, avoiding live HTTP fetches at transform time).
  -->
  <xsl:param name="collectionsUrl" as="xs:string" required="yes"/>

  <!-- $collections is evaluated once (no context item needed: doc() takes a URI param) -->
  <xsl:variable name="collections" select="doc($collectionsUrl)"/>

  <!-- ═══════════════════════════════════════════════════════════
       Root: emit the full HTML page skeleton.
       Only the <head><title> and the section outline go into the output;
       all other body text is intentionally excluded per the task spec.
       ═══════════════════════════════════════════════════════════ -->
  <xsl:template match="/">
    <!--
      $collection-code and $collection-name are resolved here (inside a template)
      where the context item IS the source document, so /country-chap is accessible.
      Placing them as top-level global variables would trigger XPDY0002 because
      global variables are evaluated without a context item in XSLT 2.0.
    -->
    <xsl:variable name="collection-code" select="/country-chap/@collection"/>
    <xsl:variable name="collection-name"
        select="$collections//collection[collection_code = $collection-code]/collection_name"/>

    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <title>
          <xsl:value-of select="concat(/country-chap/chaphead/title, ' - ', $collection-name)"/>
        </title>
        <style type="text/css">
          body  { font-family: Georgia, serif; max-width: 900px; margin: 2em auto; }
          h1,h2,h3,h4,h5,h6 { margin-top: 1.4em; }
          .label { margin-right: 0.4em; }
        </style>
      </head>
      <body>
        <xsl:apply-templates select="/country-chap/chapbody//section[not(ancestor::section)]"/>
      </body>
    </html>
  </xsl:template>

  <!-- ═══════════════════════════════════════════════════════════
       section → hN  (recursive, depth-driven)

       Depth is calculated by counting ancestor <section> elements.
       Top-level section (no ancestors) → h1, per the task example.
       Nested one level → h2, two levels → h3, etc.
       ═══════════════════════════════════════════════════════════ -->
  <xsl:template match="section">
    <xsl:variable name="depth" select="count(ancestor::section) + 1"/>
    <xsl:element name="h{$depth}">
      <span class="label">
        <xsl:value-of select="@label"/>
        <xsl:text> </xsl:text>
      </span>
      <span class="title">
        <!-- Process only direct child nodes of <title>, applying inline rules -->
        <xsl:apply-templates select="title/node()" mode="inline"/>
      </span>
    </xsl:element>
    <!-- Recurse into nested sections -->
    <xsl:apply-templates select="section"/>
  </xsl:template>

  <!-- ═══════════════════════════════════════════════════════════
       INLINE MODE – used only for content inside <title>
       ═══════════════════════════════════════════════════════════ -->

  <!-- Plain text nodes: emit as-is -->
  <xsl:template match="text()" mode="inline">
    <xsl:value-of select="."/>
  </xsl:template>

  <!-- <sub> → HTML <sub> -->
  <xsl:template match="sub" mode="inline">
    <sub>
      <xsl:apply-templates mode="inline"/>
    </sub>
  </xsl:template>

  <!-- <emph type="i"> → HTML <i> (italic) -->
  <xsl:template match="emph[@type='i']" mode="inline">
    <i>
      <xsl:apply-templates mode="inline"/>
    </i>
  </xsl:template>

  <!-- <footnote> → suppress entirely (text must NOT appear in the heading) -->
  <xsl:template match="footnote" mode="inline"/>

  <!-- Any other element inside <title> (e.g. emph type="b") – pass through text only -->
  <xsl:template match="*" mode="inline">
    <xsl:apply-templates mode="inline"/>
  </xsl:template>

  <!-- Suppress everything at the default mode that is not a section
       (paragraphs, tables, longquotes, etc. must not appear in the output) -->
  <xsl:template match="text()"/>
  <xsl:template match="*"/>

</xsl:stylesheet>
