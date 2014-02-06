/* LanguageTool, a natural language style checker
 * Copyright (C) 2014 Vincent Maubert(http://www.languagetool.org)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package org.languagetool.maven;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Execute the spellChecker LanguageTool on the languages files.
 *
 * @author <a href="mailto:v.maubert@code-troopers.com">vmaubert</a>
 * @goal check
 * @phase test
 * @requiresProject
 */
public class LanguageToolMojo extends AbstractMojo {
  /**
   * The maven project.
   *
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * Ignore or not the empty keys
   *
   * @parameter
   */
  private boolean ignoreEmptyKey = true;

  /**
   * The directory to scan for files
   *
   * @parameter default-value="${project.basedir}"
   */
  private File ressourcesDirectory;


  /**
   * The properties files to check.
   * For example :
   * <pre>
   *     <languageFiles>
   *       <languageFile>
   *         <target>messages_fr.properties</target>
   *         <language>fr</language>
   *       </languageFile>
   *      </languageFiles>
   * </pre>
   *
   * @parameter
   * @required
   */
  private LanguageFile[] languageFiles;

  /**
   * @see org.apache.maven.plugin.AbstractMojo#execute()
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    List<AbstractMojoExecutionException> exceptions = new ArrayList<AbstractMojoExecutionException>();
    boolean errorFound = false;
    for (LanguageFile languageFile : languageFiles) {
      try {
        CheckLanguageFile checkLanguageFile = new CheckLanguageFile(languageFile, ressourcesDirectory.getPath(), getLog(), ignoreEmptyKey);
        List<String> errorsList = checkLanguageFile.check();
        if (!errorsList.isEmpty()) {
          logErrors(languageFile.getTarget(), errorsList);
          errorFound = true;
        }
      } catch (AbstractMojoExecutionException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      for (AbstractMojoExecutionException exception : exceptions) {
        getLog().error(exception.getMessage());
        getLog().error(exception.getLongMessage());
      }
      throw new MojoFailureException("Unable to check properties files, please check the logs");
    }

    if (errorFound) {
      throw new MojoFailureException("You have errors in your properties files, please check the logs. " +
          "You can correct your files, write a new rule, add a new ignored word, or add an exception.");
    }

  }

  private void logErrors(String fileName, List<String> errorsList) {

    StringBuilder errorMessage = new StringBuilder();
    StringBuilder nbErrorMessage = new StringBuilder();
    nbErrorMessage.append(fileName);
    nbErrorMessage.append(" : ");
    nbErrorMessage.append(errorsList.size());
    nbErrorMessage.append(" errors. \n");

    errorMessage.append("\n* ");
    errorMessage.append(nbErrorMessage);
    for (String error : errorsList) {
      errorMessage.append(" - ");
      errorMessage.append(error);
      errorMessage.append("\n");
    }
    errorMessage.append("* ");
    errorMessage.append(nbErrorMessage);
    nbErrorMessage.append("\n");
    getLog().error(errorMessage.toString());
  }

}