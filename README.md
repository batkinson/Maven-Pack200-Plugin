# Maven Pack200 Plugin

A plugin that enables using the pack200 compression tool within Apache
Maven2+ builds, simply and flexibly.

## Why?

It was originally developed for optimizing download sizes and startup
performance of Apache Pivot-based applications deployed via Java Webstart. It
was developed carefully to provide a general solution for both signed and
unsigned jars with pack200.

There is support for pack200 in the [Webstart Maven
Plugin](http://mojo.codehaus.org/webstart/webstart-maven-plugin/), but the
plugin's approach was too rigid and monolithic, assuming and dictating too much
about how the build and deployment worked. This plugin grants full control over
using the JDK's pack200 tool, allowing one to configure and bind executions to
Maven build phases as needed directly while retaining the flexibility to package
your application as you wish.

## Requirements

To use this plugin, you'll need:

  * [Apache Maven](http://maven.apache.org/) or a compatibile tool.

## Building

To build the plugin, simply run the following:

```
# Build and install the plugin
mvn install
```

## Configuration

You configure the plugin, you configure it as you would any maven plugin, in the
project object model file, pom.xml. At a minimum, you need to declare the plugin
and bind it to your build's phases.

The following example configures the plugin to run twice:

  * Pack and unpack, so crypto signatures are valid after decompression
  * Pack the in their final optimized form

Between these two executions, you configure something to sign the jars.

```
<plugins>
   <plugin>
      <groupId>org.openxdata.plugins</groupId>
      <artifactId>maven-pack200-plugin</artifactId>
      <version>1.0</version> <!-- ensure this matches your version -->
      <executions>
         <execution>
            <id>normalize-jars</id>
            <phase>package</phase>
            <goals>
               <goal>pack</goal>
            </goals>
            <configuration>
               <normalizeOnly>true</normalizeOnly>
            </configuration>
         </execution>
         <execution>
            <id>pack-jars</id>
            <phase>verify</phase>
            <goals>
               <goal>pack</goal>
            </goals>
            <configuration>
               <normalizeOnly>false</normalizeOnly>
            </configuration>
         </execution>
      </executions>
   </plugin>
</plugins>
```

## Example

For a complete working example, take a peek at the [OXD Form
Designer
Project](https://github.com/batkinson/OXDFormDesignerMockup/blob/master/form-designer/pom.xml).
It builds an Apache Pivot-based application as a shaded (all dependencies
included) signed and optimized jar.

