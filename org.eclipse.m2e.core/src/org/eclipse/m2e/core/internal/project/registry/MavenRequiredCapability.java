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

package org.eclipse.m2e.core.internal.project.registry;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import org.eclipse.m2e.core.embedder.ArtifactKey;


public class MavenRequiredCapability extends RequiredCapability {

  private static final long serialVersionUID = 3254716937353332553L;

  private final String versionRange;

  private final String scope;

  private final boolean optional;

  private MavenRequiredCapability(String namespace, String id, String versionRange, String scope, boolean optional) {
    super(namespace, id);

    if(versionRange == null) {
      throw new NullPointerException();
    }

    this.versionRange = versionRange;
    this.scope = scope;
    this.optional = optional;
  }

  public static MavenRequiredCapability createMaven(ArtifactKey key, String scope, boolean optional) {
    return new MavenRequiredCapability(MavenCapability.NS_MAVEN_ARTIFACT, MavenCapability.getId(key), key.getVersion(),
        scope, optional);
  }

  public static MavenRequiredCapability createMavenParent(ArtifactKey key) {
    return new MavenRequiredCapability(MavenCapability.NS_MAVEN_PARENT, MavenCapability.getId(key), key.getVersion(),
        null, false);
  }

  public boolean isPotentialMatch(Capability capability) {
    if(capability instanceof MavenCapability && getVersionlessKey().equals(capability.getVersionlessKey())) {
      try {
        // TODO may need to cache parsed version and versionRange for performance reasons
        ArtifactVersion version = new DefaultArtifactVersion(((MavenCapability) capability).getVersion());
        VersionRange range = VersionRange.createFromVersionSpec(versionRange);
        return range.containsVersion(version);
      } catch(InvalidVersionSpecificationException ex) {
        return true; // better safe than sorry
      }
    }
    return false;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getVersionlessKey().toString());
    if(scope != null) {
      sb.append(':').append(scope);
    }
    sb.append('/').append(versionRange);
    if(optional) {
      sb.append("(optional)");
    }
    return sb.toString();
  }

  public int hashCode() {
    int hash = getVersionlessKey().hashCode();
    hash = hash * 17 + versionRange.hashCode();
    hash = hash * 17 + (scope != null ? scope.hashCode() : 0);
    hash = hash * 17 + (optional ? 1 : 0);
    return hash;
  }

  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(!(obj instanceof MavenRequiredCapability)) {
      return false;
    }
    MavenRequiredCapability other = (MavenRequiredCapability) obj;
    return getVersionlessKey().equals(other.getVersionlessKey()) && versionRange.equals(other.versionRange)
        && eq(scope, other.scope) && optional == other.optional;
  }

}
