/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.views.nodes;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;

import org.eclipse.m2e.core.MavenImages;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.index.NexusIndex;
import org.eclipse.m2e.core.internal.index.NexusIndexManager;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


/**
 * Parent node for all artifact repositories configured in pom.xml files.
 */
public class ProjectRepositoriesNode implements IMavenRepositoryNode {

  private NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getDefault().getIndexManager();
  private IRepositoryRegistry repositoryRegistry = MavenPlugin.getDefault().getRepositoryRegistry();

  public Object[] getChildren() {
    ArrayList<Object> nodes = new ArrayList<Object>();
    for(IRepository repo : repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_PROJECT)) {
      NexusIndex index = indexManager.getIndex(repo);
      RepositoryNode node = new RepositoryNode(index);
      nodes.add(node);
    }
    return nodes.toArray(new Object[nodes.size()]);
  }

  public Image getImage() {
    return MavenImages.IMG_INDEXES;
  }

  public String getName() {
    return "Project Repositories";
  }

  public String toString() {
    return getName();
  }

  public boolean hasChildren() {
    Object[] kids = getChildren();
    return kids != null && kids.length > 0;
  }

  public boolean isUpdating() {
    return false;
  }

}
