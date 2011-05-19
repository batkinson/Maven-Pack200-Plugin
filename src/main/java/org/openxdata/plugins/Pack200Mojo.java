package org.openxdata.plugins;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal capable of compressing a jar with the pack200 tool.
 * 
 * @goal pack
 * 
 * @phase package
 */
public class Pack200Mojo extends AbstractMojo {

	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * @parameter expression="${project.build.finalName}.${project.packaging}"
	 */
	private String finalName;

	public void execute() throws MojoExecutionException {

		Packer packer = Pack200.newPacker();

		Map packerProps = packer.properties();

		packerProps.put(Packer.EFFORT, "7");
		packerProps.put(Packer.SEGMENT_LIMIT, "-1");
		packerProps.put(Packer.KEEP_FILE_ORDER, Packer.FALSE);
		packerProps.put(Packer.MODIFICATION_TIME, Packer.LATEST);
		packerProps.put(Packer.DEFLATE_HINT, Packer.FALSE);
		packerProps.put(Packer.CODE_ATTRIBUTE_PFX + "LineNumerTable",
				Packer.STRIP);
		packerProps.put(Packer.UNKNOWN_ATTRIBUTE, Packer.ERROR);

		File file = new File(outputDirectory, finalName);
		try {
			JarFile jar = new JarFile(file);
			FileOutputStream fos = new FileOutputStream(file.getAbsolutePath()
					+ ".packed");
			packer.pack(jar, fos);
			jar.close();
			fos.close();
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to pack jar.", e);
		}
	}
}
