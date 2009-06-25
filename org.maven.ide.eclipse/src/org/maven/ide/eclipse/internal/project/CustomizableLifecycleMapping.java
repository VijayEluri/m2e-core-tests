/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
import org.maven.ide.eclipse.project.configurator.AbstractLifecycleMapping;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ILifecycleMapping;
import org.maven.ide.eclipse.project.configurator.MojoExecutionBuildParticipant;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * CustomizableLifecycleMapping
 * 
 * @author igor
 */
public class CustomizableLifecycleMapping extends AbstractLifecycleMapping implements ILifecycleMapping {
  public static final String EXTENSION_ID = "customizable";
  
  private final List<AbstractProjectConfigurator> configurators;
  
  public CustomizableLifecycleMapping() {
    configurators = null;
  }
  
  public CustomizableLifecycleMapping(Element configurationNode) {
    this.configurators = parseFromDOM(configurationNode);
  }
  
  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {
    if(configurators != null) {
      return configurators;
    }
    MavenProject mavenProject = facade.getMavenProject(monitor);
    Plugin plugin = mavenProject.getPlugin("org.maven.ide.eclipse:lifecycle-mapping");

    if(plugin == null) {
      throw new IllegalArgumentException("no mapping");
    }

    // TODO assert version

    Map<String, AbstractProjectConfigurator> configuratorsMap = new LinkedHashMap<String, AbstractProjectConfigurator>();
    for(AbstractProjectConfigurator configurator : getProjectConfigurators(false)) {
      configuratorsMap.put(configurator.getId(), configurator);
    }

    Xpp3Dom config = (Xpp3Dom) plugin.getConfiguration();

    if(config == null) {
      throw new IllegalArgumentException("Empty lifecycle mapping configuration");
    }

    Xpp3Dom configuratorsDom = config.getChild("configurators");
    Xpp3Dom executionsDom = config.getChild("mojoExecutions");

    List<AbstractProjectConfigurator> configurators = new ArrayList<AbstractProjectConfigurator>();
    
    if (configuratorsDom != null) {
      for(Xpp3Dom configuratorDom : configuratorsDom.getChildren("configurator")) {
        String configuratorId = configuratorDom.getAttribute("id");
        AbstractProjectConfigurator configurator = configuratorsMap.get(configuratorId);
        if(configurator == null) {
          throw new IllegalArgumentException("Unknown configurator id=" + configuratorId);
        }

        configurators.add(configurator);
      }
    }
    
    if (executionsDom != null) {
      for(Xpp3Dom execution : executionsDom.getChildren("mojoExecution")) {
        String strRunOnClean = execution.getAttribute("runOnClean");
        String strRunOnIncremental = execution.getAttribute("runOnIncremental");
        configurators.add(MojoExecutionProjectConfigurator.fromString(execution.getValue(), toBool(strRunOnIncremental, true), toBool(strRunOnClean, true)));
      }
    }

    return configurators;
  }
  
  private List<AbstractProjectConfigurator> parseFromDOM(Element configNode) {
    
    Map<String, AbstractProjectConfigurator> configuratorsMap = new LinkedHashMap<String, AbstractProjectConfigurator>();
    for(AbstractProjectConfigurator configurator : getProjectConfigurators(false)) {
      configuratorsMap.put(configurator.getId(), configurator);
    }

    Element configuratorsDom = getChildElement(configNode, "configurators");
    Element executionsDom = getChildElement(configNode, "mojoExecutions");

    List<AbstractProjectConfigurator> configurators = new ArrayList<AbstractProjectConfigurator>();
    
    if (configuratorsDom != null) {
      for(Element configuratorDom : getChildren(configuratorsDom, "configurator")) {
        String configuratorId = configuratorDom.getAttribute("id");
        AbstractProjectConfigurator configurator = configuratorsMap.get(configuratorId);
        if(configurator == null) {
          throw new IllegalArgumentException("Unknown configurator id=" + configuratorId);
        }

        configurators.add(configurator);
      }
    }
    
    if (executionsDom != null) {
      for(Element execution : getChildren(executionsDom, "mojoExecution")) {
        String strRunOnClean = execution.getAttribute("runOnClean");
        String strRunOnIncremental = execution.getAttribute("runOnIncremental");
        configurators.add(MojoExecutionProjectConfigurator.fromString(getNodeContents(execution), toBool(strRunOnIncremental, true), toBool(strRunOnClean, true)));
      }
    }
    
    return configurators;
  }
  
  private boolean toBool(String value, boolean def) {
    if(value == null || value.length() == 0) {
      return def;
    }
    return Boolean.parseBoolean(value);
  }
  
  private Element getChildElement(Element parent, String name) {
    Node n = parent.getFirstChild();
    while(n != null) {
      if(n instanceof Element && n.getNodeName().equals(name)) {
        return (Element)n;
      }
      n = n.getNextSibling();
    }
    return null;
  }
  
  private List<Element> getChildren(Element parent, String name) {
    List<Element> ret = new LinkedList<Element>();
    Node n = parent.getFirstChild();
    while(n != null) {
      if(n instanceof Element && n.getNodeName().equals(name)) {
        ret.add((Element)n);
      }
      n = n.getNextSibling();
    }
    return ret;
  }
  
  private String getNodeContents(Node n) {
    if(n instanceof Text) {
      return ((Text)n).getNodeValue();
    } else if(n instanceof Element) {
      StringBuilder value = new StringBuilder();
      Node child = ((Element)n).getFirstChild();
      while(child != null) {
        value.append(getNodeContents(child));
        child = child.getNextSibling();
      }
      return value.toString();
    }
    return "";
  }

  private Set<String> getListElements(Xpp3Dom listDom, String elementName) {
    Set<String> elements = new LinkedHashSet<String>();
    if (listDom == null) {
      return elements;
    }

    for (Xpp3Dom elementDom : listDom.getChildren(elementName)) {
      elements.add(elementDom.getValue());
    }

    return elements;
  }

  public List<AbstractBuildParticipant> getBuildParticipants(IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {

    List<AbstractProjectConfigurator> configurators = getProjectConfigurators(facade, monitor);

    List<AbstractBuildParticipant> participants = new ArrayList<AbstractBuildParticipant>();

    for (MojoExecution execution : facade.getExecutionPlan(monitor).getExecutions()) {
      for (AbstractProjectConfigurator configurator : configurators) {
        AbstractBuildParticipant participant = configurator.getBuildParticipant(execution);
        if (participant != null) {
          participants.add(participant);
        }
      }
    }

    return participants;
  }
  
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    super.configure(request, monitor);

    addMavenBuilder(request.getProject(), monitor);
  }
  
  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.project.configurator.ILifecycleMapping#getPotentialMojoExecutionsForBuildKind(org.maven.ide.eclipse.project.IMavenProjectFacade, int, org.eclipse.core.runtime.IProgressMonitor)
   */
  public List<String> getPotentialMojoExecutionsForBuildKind(IMavenProjectFacade projectFacade, int kind,
      IProgressMonitor progressMonitor) {
    List<String> mojos = new LinkedList<String>();
    try {
      for (MojoExecution execution : projectFacade.getExecutionPlan(progressMonitor).getExecutions()) {
        for (AbstractProjectConfigurator configurator : getProjectConfigurators(projectFacade, progressMonitor)) {
          AbstractBuildParticipant participant = configurator.getBuildParticipant(execution);
          if (participant != null && participant instanceof MojoExecutionBuildParticipant) {
            if(((MojoExecutionBuildParticipant)participant).appliesToBuildKind(kind)) {
              MojoExecution mojo = ((MojoExecutionBuildParticipant)participant).getMojoExecution();
              mojos.add(MojoExecutionUtils.getExecutionKey(mojo));
            }
          }
        }
      }
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
    return mojos;
  }

}