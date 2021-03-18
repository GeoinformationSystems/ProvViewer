<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs"
                xmlns:prov="http://www.w3.org/ns/prov#"
                xmlns:graph="https://jgraph.github.io/mxgraph/" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="https://colabis.de/" version="1.0"
                xsi:schemaLocation="http://www.w3.org/ns/prov# ">
    <xsl:output indent="yes" omit-xml-declaration="yes"/>
    <xsl:template match="/">
        <mxGraphModel>
            <root>
                <Diagram label="My Diagram" href="http://www.jgraph.com/" id="0">
                    <mxCell/>
                </Diagram>
                <Layer label="Default Layer" id="1">
                    <mxCell parent="0"/>
                </Layer>
                <xsl:apply-templates/>
            </root>
        </mxGraphModel>
    </xsl:template>
    <!-- prov:entity -->
    <xsl:template match="prov:entity[not(./@prov:ref)]">
        <xsl:variable name="label" select="./@prov:id"/>
        <DataDMP label="{translate(./@prov:id, '_', ' ')}" href="" id="{./graph:information/@graph:id}">
            <xsl:copy-of select="./graph:metadata/@*"/>
            <xsl:copy-of select="./graph:information/node()"/>
        </DataDMP>
    </xsl:template>
    <!-- prov:activity -->
    <xsl:template match="prov:activity[not(./@prov:ref)]">
        <ProcessDMP label="{translate(./@prov:id, '_', ' ')}" href="" id="{./graph:information/@graph:id}">
            <xsl:copy-of select="./graph:metadata/@*"/>
            <xsl:copy-of select="./graph:information/node()"/>
        </ProcessDMP>
    </xsl:template>
    <!-- prov:organization -->
    <xsl:template match="prov:organization[not(./@prov:ref)]">
        <ActorDMP label="{translate(./@prov:id, '_', ' ')}" href="" id="{./graph:information/@graph:id}">
            <xsl:copy-of select="./graph:metadata/@*"/>
            <xsl:copy-of select="./graph:information/node()"/>
        </ActorDMP>
    </xsl:template>

    <xsl:template match="prov:wasAssociatedWith|prov:wasAttributedTo|prov:used|prov:wasGeneratedBy">
        <Connector label="" href="" id="{./graph:information/@graph:id}">
            <xsl:copy-of select="./graph:metadata/@*"/>
            <xsl:copy-of select="./graph:information/node()"/>
        </Connector>
    </xsl:template>
</xsl:stylesheet>