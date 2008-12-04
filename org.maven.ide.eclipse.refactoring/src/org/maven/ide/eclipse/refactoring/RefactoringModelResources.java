/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.PropertyPair;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.IMavenProjectFacade;

/**
 * This class manages all refactoring-related resources for a particular maven project
 * 
 * @author Anton Kraev
 */
public class RefactoringModelResources {
  private static final String TMP_PROJECT_NAME = ".m2eclipse_refactoring";
  protected IFile pomFile;
  protected IFile tmpFile;
  protected ITextFileBuffer pomBuffer;
  protected ITextFileBuffer tmpBuffer;
  protected Model tmpModel;
  protected org.apache.maven.model.Model effective;
  protected ITextFileBufferManager textFileBufferManager;
  protected Map<String, PropertyInfo> properties;
  protected MavenProject project;
  protected CompoundCommand command;
  protected static IProject tmpProject;
  
  protected IProject getTmpProject() {
    if (tmpProject == null) {
      tmpProject = ResourcesPlugin.getWorkspace().getRoot().getProject(TMP_PROJECT_NAME);
    }
    if (!tmpProject.exists()) {
      try {
        tmpProject.create(null);
        tmpProject.open(null);
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }
    return tmpProject;
  }
  
  public RefactoringModelResources(IMavenProjectFacade projectFacade) throws CoreException, IOException {
    textFileBufferManager = FileBuffers.getTextFileBufferManager();
    project = projectFacade.getMavenProject(null);
    effective = project.getModel();
    pomFile = projectFacade.getPom();
    pomBuffer = getBuffer(pomFile);

    //create temp file
    IProject project = getTmpProject();
    File f = File.createTempFile("pom", ".xml", project.getLocation().toFile());
    f.delete();
    tmpFile = project.getFile(f.getName());
    pomFile.copy(tmpFile.getFullPath(), true, null);
    
    tmpModel = loadModel(tmpFile);
    tmpBuffer = getBuffer(tmpFile);
  }

  public static Model loadModel(IFile file) throws CoreException {
    return MavenPlugin.getDefault().getMavenModelManager().loadResource(file).getModel();
  }
  
  public CompoundCommand getCommand() {
    return command;
  }

  public void setCommand(CompoundCommand command) {
    this.command = command;
  }

  public IFile getPomFile() {
    return pomFile;
  }

  public IFile getTmpFile() {
    return tmpFile;
  }

  public ITextFileBuffer getPomBuffer() {
    return pomBuffer;
  }

  public ITextFileBuffer getTmpBuffer() {
    return tmpBuffer;
  }

  public Model getTmpModel() {
    return tmpModel;
  }

  public org.apache.maven.model.Model getEffective() {
    return effective;
  }

  public MavenProject getProject() {
    return project;
  }

  public Map<String, PropertyInfo> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, PropertyInfo> properties) {
    this.properties = properties;
  }

  public void releaseAllResources() throws CoreException {
    releaseBuffer(pomBuffer, pomFile);
    if (tmpFile != null && tmpFile.exists()) {
      releaseBuffer(tmpBuffer, tmpFile);
    }
  }

  public static void cleanupTmpProject() throws CoreException {
    if (tmpProject.exists()) {
      tmpProject.delete(true, true, null);
    }
  }
    
  
  protected ITextFileBuffer getBuffer(IFile file) throws CoreException {
    textFileBufferManager.connect(file.getLocation(), LocationKind.NORMALIZE, null);
    return textFileBufferManager.getTextFileBuffer(file.getLocation(), LocationKind.NORMALIZE); 
  }

  protected void releaseBuffer(ITextFileBuffer buffer, IFile file) throws CoreException {
    buffer.revert(null);
    textFileBufferManager.disconnect(file.getLocation(), LocationKind.NORMALIZE, null);
  }

  public String getName() {
    return pomFile.getProject().getName();
  }

  public static class PropertyInfo {
    protected PropertyPair pair;
    protected RefactoringModelResources resource;
    protected Command newValue;
    
    public Command getNewValue() {
      return newValue;
    }

    public void setNewValue(Command newValue) {
      this.newValue = newValue;
    }
    
    public PropertyPair getPair() {
      return pair;
    }

    public void setPair(PropertyPair pair) {
      this.pair = pair;
    }

    public RefactoringModelResources getResource() {
      return resource;
    }

    public void setResource(RefactoringModelResources resource) {
      this.resource = resource;
    }
  }

}
