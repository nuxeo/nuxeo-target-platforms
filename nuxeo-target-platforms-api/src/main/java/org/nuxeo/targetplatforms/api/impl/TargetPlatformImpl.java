/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPlatform;

/**
 * @since 5.7.1
 */
public class TargetPlatformImpl extends TargetImpl implements TargetPlatform {

    private static final long serialVersionUID = 1L;

    protected TargetPlatform parent;

    protected boolean fastTrack = false;

    protected boolean trial = false;

    protected Map<String, TargetPackage> availablePackages;

    protected List<String> testVersions;

    protected TargetPlatformImpl() {
        super();
    }

    public TargetPlatformImpl(String id) {
        super(id);
    }

    public TargetPlatformImpl(String id, String name, String version,
            String refVersion, String label) {
        super(id, name, version, refVersion, label);
    }

    @Override
    public boolean isFastTrack() {
        return fastTrack;
    }

    public void setFastTrack(boolean fastTrack) {
        this.fastTrack = fastTrack;
    }

    @Override
    public List<String> getAvailablePackagesIds() {
        if (availablePackages == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(availablePackages.keySet());
    }

    @Override
    public List<TargetPackage> getAvailablePackages() {
        if (availablePackages == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(availablePackages.values());
    }

    public void addAvailablePackage(TargetPackage pack) {
        if (pack == null) {
            return;
        }
        if (availablePackages == null) {
            availablePackages = new LinkedHashMap<>();
        }
        availablePackages.put(pack.getId(), pack);
    }

    public void setAvailablePackages(Map<String, TargetPackage> tps) {
        if (availablePackages == null) {
            availablePackages = new LinkedHashMap<>();
        } else {
            availablePackages.clear();
        }
        if (tps != null) {
            availablePackages.putAll(tps);
        }
    }

    public TargetPlatform getParent() {
        return parent;
    }

    public void setParent(TargetPlatform parent) {
        this.parent = parent;
    }

    public List<String> getTestVersions() {
        return testVersions;
    }

    public void setTestVersions(List<String> testVersions) {
        if (testVersions == null) {
            this.testVersions = testVersions;
        } else {
            // dereference
            this.testVersions = new ArrayList<>(testVersions);
        }
    }

    public boolean isTrial() {
        return trial;
    }

    public void setTrial(boolean trial) {
        this.trial = trial;
    }

    @Override
    public int compareTo(TargetPlatform o) {
        // compare first on name, then on version
        int comp = getName().compareTo(o.getName());
        if (comp == 0) {
            comp = getVersion().compareTo(o.getVersion());
        }
        return comp;
    }

}