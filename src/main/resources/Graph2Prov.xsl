<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs"
                xmlns:prov="http://www.w3.org/ns/prov#"
                xmlns:graph="https://jgraph.github.io/mxgraph/" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="https://colabis.de/" version="1.0">
    <xsl:output method="xml"/>
    <!--    <xsl:strip-space elements="*"/>-->
    <xsl:template match="/">
        <prov:document xmlns:prov="http://www.w3.org/ns/prov#"
                       xmlns:graph="https://jgraph.github.io/mxgraph/"
                       xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xmlns="http://example.org/">
            <xsl:apply-templates/>
        </prov:document>
    </xsl:template>
    <!-- prov:entity -->
    <xsl:template match="DataDMP">
        <prov:entity prov:id="{translate(./@label, ' ', '_')}">
            <prov:type xsi:type="xsd:QName">
                <xsl:value-of select="translate(./@label, ' ', '_')"/>
            </prov:type>
            <graph:information graph:id="{./@id}">
                <xsl:copy-of select="./node()"/>
            </graph:information>
            <graph:metadata>
                <xsl:copy-of select="@*[name()!='label'][name()!='href'][name()!='id']"/>
            </graph:metadata>
        </prov:entity>
    </xsl:template>
    <!-- prov:activity -->
    <xsl:template match="ProcessDMP">
        <prov:activity prov:id="{translate(./@label, ' ', '_')}">
            <graph:information graph:id="{./@id}">
                <xsl:copy-of select="./node()"/>
            </graph:information>
            <graph:metadata>
                <xsl:copy-of select="@*[name()!='label'][name()!='href'][name()!='id']"/>
            </graph:metadata>
        </prov:activity>
    </xsl:template>
    <!-- prov:organization -->
    <xsl:template match="ActorDMP">
        <prov:organization prov:id="{translate(./@label, ' ', '_')}">
            <graph:information graph:id="{./@id}">
                <xsl:copy-of select="./node()"/>
            </graph:information>
            <graph:metadata>
                <xsl:copy-of select="@*[name()!='label'][name()!='href'][name()!='id']"/>
            </graph:metadata>
        </prov:organization>
    </xsl:template>
    <!-- Connections between prov elements -->
    <xsl:template match="Connector">
        <xsl:variable name="source" select="./mxCell/@source"/>
        <xsl:variable name="target" select="./mxCell/@target"/>
        <xsl:variable name="lid" select="./@label"/>

        <xsl:choose>
            <!-- prov:wasAssociatedWith -->
            <xsl:when test="//ActorDMP/@id = $source and //ProcessDMP/@id = $target">
                <prov:wasAssociatedWith>
                    <prov:activity prov:ref="{translate(//ProcessDMP[@id=$target]/@label,' ','_')}"/>
                    <prov:agent prov:ref="{translate(//ActorDMP[@id=$source]/@label,' ','_')}"/>
                    <graph:information graph:id="{./@id}">
                        <xsl:copy-of select="./node()"/>
                    </graph:information>
                    <graph:metadata>
                        <xsl:copy-of select="@*[name()!='label'][name()!='href'][name()!='id']"/>
                    </graph:metadata>
                </prov:wasAssociatedWith>
            </xsl:when>
            <!-- prov:wasAttributedTo -->
            <xsl:when test="//ActorDMP/@id = $source and //DataDMP/@id = $target">
                <prov:wasAttributedTo>
                    <prov:entity prov:ref="{translate(//DataDMP[@id=$target]/@label, ' ', '_')}"/>
                    <prov:agent prov:ref="{translate(//ActorDMP[@id=$source]/@label, ' ', '_')}"/>
                    <graph:information graph:id="{./@id}">
                        <xsl:copy-of select="./node()"/>
                    </graph:information>
                    <graph:metadata>
                        <xsl:copy-of select="@*[name()!='label'][name()!='href'][name()!='id']"/>
                    </graph:metadata>
                </prov:wasAttributedTo>
            </xsl:when>
            <!-- prov:used -->
            <xsl:when test="//DataDMP/@id = $source and //ProcessDMP/@id = $target">
                <prov:used>
                    <prov:activity prov:ref="{translate(//ProcessDMP[@id=$target]/@label, ' ', '_')}"/>
                    <prov:entity prov:ref="{translate(//DataDMP[@id=$source]/@label, ' ', '_')}"/>
                    <graph:information graph:id="{./@id}">
                        <xsl:copy-of select="./node()"/>
                    </graph:information>
                    <graph:metadata>
                        <xsl:copy-of select="@*[name()!='label'][name()!='href'][name()!='id']"/>
                    </graph:metadata>
                </prov:used>
            </xsl:when>
            <!-- prov:wasGenereatedBy -->
            <xsl:when test="//ProcessDMP/@id = $source and //DataDMP/@id = $target">
                <prov:wasGeneratedBy>
                    <prov:entity prov:ref="{translate(//DataDMP[@id=$target]/@label, ' ', '_')}"/>
                    <prov:activity prov:ref="{translate(//ProcessDMP[@id=$source]/@label, ' ', '_')}"/>
                    <graph:information graph:id="{./@id}">
                        <xsl:copy-of select="./node()"/>
                    </graph:information>
                    <graph:metadata>
                        <xsl:copy-of select="@*[name()!='label'][name()!='href'][name()!='id']"/>
                    </graph:metadata>
                </prov:wasGeneratedBy>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
