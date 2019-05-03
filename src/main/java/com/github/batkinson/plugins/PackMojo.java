package com.github.batkinson.plugins;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;
import java.util.jar.Pack200.Unpacker;
import java.util.zip.GZIPOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

/**
 * Goal capable of compressing a jar with the pack200 tool.
 *
 * @goal pack
 * @phase package
 */
public class PackMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    MavenProject project;

    /**
     * @parameter default-value=""
     */
    private boolean processMainArtifact;

    /**
     * @parameter default-value=""
     */
    private Set<String> includeClassifiers;

    /**
     * @parameter default-value="true"
     */
    private boolean attachPackedArtifacts;

    /**
     * @parameter default-value="false"
     */
    private boolean normalizeOnly;

    /**
     * @parameter default-value="7"
     */
    private String effort;

    /**
     * @parameter default-value="-1"
     */
    private String segmentLimit;

    /**
     * @parameter default-value="false"
     */
    private String keepFileOrder;

    /**
     * @parameter default-value="latest"
     */
    private String modificationTime;

    /**
     * @parameter default-value="false"
     */
    private String deflateHint;

    /**
     * @parameter
     */
    private String[] stripCodeAttributes = {"SourceFile", "LineNumberTable",
            "LocalVariableTable", "Deprecated"};

    /**
     * @parameter default-value="true"
     */
    private boolean failOnUnknownAttributes;

    /**
     * @parameter default-value="true"
     */
    private boolean compress;

    /**
     * @parameter
     */
    private String[] passFiles = {};

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException {

        getLog().debug("starting pack");

        Packer packer = Pack200.newPacker();
        Unpacker unpacker = null;

        if (normalizeOnly)
            unpacker = Pack200.newUnpacker();

        String type = "jar.pack";
        if (compress)
            type = "jar.pack.gz";

        Map<String, String> packerProps = packer.properties();

        packerProps.put(Packer.EFFORT, effort);
        packerProps.put(Packer.SEGMENT_LIMIT, segmentLimit);
        packerProps.put(Packer.KEEP_FILE_ORDER, keepFileOrder);
        packerProps.put(Packer.MODIFICATION_TIME, modificationTime);
        packerProps.put(Packer.DEFLATE_HINT, deflateHint);

        if (stripCodeAttributes != null) {
            for (String attributeName : stripCodeAttributes) {
                getLog().debug("stripping " + attributeName);
                packerProps.put(Packer.CODE_ATTRIBUTE_PFX + attributeName,
                        Packer.STRIP);
            }
        }

        if (passFiles != null) {
            int pos = 0;
            for (String passFile : passFiles) {
                getLog().debug("bypassing " + passFile);
                packerProps.put(Packer.PASS_FILE_PFX + pos, passFile);
                pos++;
            }
        }

        if (failOnUnknownAttributes)
            packerProps.put(Packer.UNKNOWN_ATTRIBUTE, Packer.ERROR);

        List<Artifact> artifactsToPack = new ArrayList<Artifact>();

        if (processMainArtifact)
            artifactsToPack.add(project.getArtifact());

        if (includeClassifiers != null && !includeClassifiers.isEmpty()) {
            for (Artifact secondaryArtifact : (List<Artifact>) project
                    .getAttachedArtifacts())
                if ("jar".equals(secondaryArtifact.getType())
                        && secondaryArtifact.hasClassifier()
                        && includeClassifiers.contains(secondaryArtifact
                        .getClassifier()))
                    artifactsToPack.add(secondaryArtifact);
        }

        String action = normalizeOnly ? "normalizing" : "packing";

        getLog().info(action + " " + artifactsToPack.size() + " artifacts");

        for (Artifact artifactToPack : artifactsToPack) {

            File origFile = artifactToPack.getFile();
            File packFile = new File(origFile.getAbsolutePath() + ".pack");
            File zipFile = new File(packFile.getAbsolutePath() + ".gz");

            try {
                JarFile jar = new JarFile(origFile);
                FileOutputStream fos = new FileOutputStream(packFile);

                getLog().debug("packing " + origFile + " to " + packFile);
                packer.pack(jar, fos);

                getLog().debug("closing handles ...");
                jar.close();
                fos.close();

                if (normalizeOnly) {
                    getLog().debug("unpacking " + packFile + " to " + origFile);
                    JarOutputStream origJarStream = new JarOutputStream(
                            new FileOutputStream(origFile));
                    unpacker.unpack(packFile, origJarStream);

                    getLog().debug("closing handles...");
                    origJarStream.close();

                    getLog().debug("unpacked file");
                } else {

                    Artifact newArtifact = new DefaultArtifact(
                            artifactToPack.getGroupId(),
                            artifactToPack.getArtifactId(),
                            artifactToPack.getVersionRange(),
                            artifactToPack.getScope(), type,
                            artifactToPack.getClassifier(),
                            new DefaultArtifactHandler(type));

                    if (compress) {
                        getLog().debug(
                                "compressing " + packFile + " to " + zipFile);
                        GZIPOutputStream zipOut = new GZIPOutputStream(
                                new FileOutputStream(zipFile));
                        IOUtil.copy(new FileInputStream(packFile), zipOut);
                        zipOut.close();
                        newArtifact.setFile(zipFile);
                    } else {
                        newArtifact.setFile(packFile);
                    }

                    if (attachPackedArtifacts) {
                        getLog().debug("attaching " + newArtifact);
                        project.addAttachedArtifact(newArtifact);
                    }
                }

                getLog().debug("finished " + action + " " + packFile);

            } catch (IOException e) {
                throw new MojoExecutionException("Failed to pack jar.", e);
            }
        }
    }
}
