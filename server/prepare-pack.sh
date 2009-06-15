#!/bin/sh

#
# Delete some entries from the JAR files in the target/binaries directory.
# This script MUST be invoked from the target/binaries directory - it will
# not be checked. The entries below are removed because duplicated entries
# cause the creation of PACK200 file to fail.
#

#
# Remove entries that duplicate commons-collections-3.2.1.jar.
#
zip -d commons-beanutils-1.7.0.jar org/apache/commons/collections/*

#
# Remove entries that duplicate batik-ext-1.7.jar and JDK 6.
#
zip -d xml-apis-1.3.04.jar org/w3c/dom/events/*

#
# Remove entries that duplicate xalan-2.6.0.jar
#
zip -d xml-apis-1.3.04.jar org/w3c/dom/xpath/*

#
# Remove entries that duplicate netcdf-2.2.20.jar.
#
zip -d grib-5.1.03.jar ucar/unidata/io/*

#
# Remove duplicate entries
#
find . -name "*.jar" -exec zip -d '{}' NOTICE LICENSE README license/* META-INF/*.txt META-INF/maven/* \;

#
# The command below needs to be executed from the root directory.
#
# mvn org.geotoolkit.project:geotk-jar-collector:pack --non-recursive
