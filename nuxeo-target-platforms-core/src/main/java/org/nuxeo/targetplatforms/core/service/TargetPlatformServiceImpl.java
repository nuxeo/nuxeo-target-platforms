/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.core.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPackageInfo;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;
import org.nuxeo.targetplatforms.api.impl.TargetPackageImpl;
import org.nuxeo.targetplatforms.api.impl.TargetPackageInfoImpl;
import org.nuxeo.targetplatforms.api.impl.TargetPlatformImpl;
import org.nuxeo.targetplatforms.api.impl.TargetPlatformInfoImpl;
import org.nuxeo.targetplatforms.api.impl.TargetPlatformInstanceImpl;
import org.nuxeo.targetplatforms.api.service.TargetPlatformService;
import org.nuxeo.targetplatforms.core.descriptors.ServiceConfigurationDescriptor;
import org.nuxeo.targetplatforms.core.descriptors.TargetPackageDescriptor;
import org.nuxeo.targetplatforms.core.descriptors.TargetPlatformDescriptor;


/**
 * {@link TargetPlatformService} implementation relying on runtime extension
 * points.
 *
 * @since 2.18
 */
public class TargetPlatformServiceImpl extends DefaultComponent implements
        TargetPlatformService {

    private static final Log log = LogFactory.getLog(TargetPlatformServiceImpl.class);

    public static final String XP_CONF = "configuration";

    public static final String XP_PLATFORMS = "platforms";

    public static final String XP_PACKAGES = "packages";

    protected static final DateTimeFormatter dateParser = DateTimeFormat.forPattern(
            "yyyy/MM/dd").withLocale(Locale.ENGLISH);

    protected ServiceConfigurationRegistry conf;

    protected TargetPlatformRegistry platforms;

    protected TargetPackageRegistry packages;

    // Runtime component API

    @Override
    public void activate(ComponentContext context) {
        platforms = new TargetPlatformRegistry();
        packages = new TargetPackageRegistry();
        conf = new ServiceConfigurationRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        platforms = null;
        packages = null;
        conf = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_PLATFORMS.equals(extensionPoint)) {
            TargetPlatformDescriptor desc = (TargetPlatformDescriptor) contribution;
            log.info(String.format("Register target platform '%s'",
                    desc.getId()));
            platforms.addContribution(desc);
        } else if (XP_PACKAGES.equals(extensionPoint)) {
            TargetPackageDescriptor desc = (TargetPackageDescriptor) contribution;
            log.info(String.format("Register target package '%s'", desc.getId()));
            packages.addContribution(desc);
        } else if (XP_CONF.equals(extensionPoint)) {
            ServiceConfigurationDescriptor desc = (ServiceConfigurationDescriptor) contribution;
            log.info(String.format("Register TargetPlatformService configuration"));
            conf.addContribution(desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_PLATFORMS.equals(extensionPoint)) {
            TargetPlatformDescriptor desc = (TargetPlatformDescriptor) contribution;
            log.info(String.format("Unregister target platform '%s'",
                    desc.getId()));
            platforms.removeContribution(desc);
        } else if (XP_PACKAGES.equals(extensionPoint)) {
            TargetPackageDescriptor desc = (TargetPackageDescriptor) contribution;
            log.info(String.format("Unregister target package '%s'",
                    desc.getId()));
            packages.removeContribution(desc);
        } else if (XP_CONF.equals(extensionPoint)) {
            ServiceConfigurationDescriptor desc = (ServiceConfigurationDescriptor) contribution;
            log.info(String.format("Unregister TargetPlatformService configuration"));
            conf.removeContribution(desc);
        }
    }

    // Service API

    @Override
    public TargetPlatform getDefaultTargetPlatform() {
        ServiceConfigurationDescriptor desc = conf.getConfiguration();
        if (desc == null) {
            return null;
        }
        String id = desc.getDefaultTargetPlatform();
        return getTargetPlatform(id);
    }

    @Override
    public TargetPlatform getTargetPlatform(String id) {
        if (id == null) {
            return null;
        }
        TargetPlatformDescriptor desc = platforms.getTargetPlatform(id);
        return getTargetPlatform(desc);
    }

    protected TargetPlatform getTargetPlatform(TargetPlatformDescriptor desc) {
        if (desc == null) {
            return null;
        }
        String id = desc.getId();
        TargetPlatformImpl tp = new TargetPlatformImpl(id, desc.getName(),
                desc.getVersion(), desc.getRefVersion(), desc.getLabel());
        tp.setDeprecated(desc.isDeprecated());
        tp.setDescription(desc.getDescription());
        tp.setDownloadLink(desc.getDownloadLink());
        tp.setEnabled(desc.isEnabled());
        tp.setEndOfAvailability(toCalendar(desc.getEndOfAvailability()));
        tp.setFastTrack(desc.isFastTrack());
        tp.setParent(getTargetPlatform(desc.getParent()));
        tp.setRefVersion(desc.getRefVersion());
        tp.setReleaseDate(toCalendar(desc.getReleaseDate()));
        tp.setRestricted(desc.isRestricted());
        tp.setStatus(desc.getStatus());
        tp.setTestVersions(desc.getTestVersions());
        tp.setTypes(desc.getTypes());
        // resolve available packages
        tp.setAvailablePackages(getTargetPackages(id));
        return tp;
    }

    /**
     * Lookup all packages referencing this target platform.
     */
    protected Map<String, TargetPackage> getTargetPackages(String targetPlatform) {
        Map<String, TargetPackage> tps = new HashMap<>();
        List<TargetPackageDescriptor> pkgs = packages.getTargetPackages(targetPlatform);
        if (pkgs != null) {
            for (TargetPackageDescriptor pkg : pkgs) {
                TargetPackage tp = getTargetPackage(pkg);
                if (tp != null) {
                    tps.put(tp.getId(), tp);
                }
            }
        }
        return tps;
    }

    protected Map<String, TargetPackageInfo> getTargetPackagesInfo(
            String targetPlatform) {
        Map<String, TargetPackageInfo> tps = new HashMap<>();
        List<TargetPackageDescriptor> pkgs = packages.getTargetPackages(targetPlatform);
        if (pkgs != null) {
            for (TargetPackageDescriptor pkg : pkgs) {
                TargetPackageInfo tp = getTargetPackageInfo(pkg.getId());
                if (tp != null) {
                    tps.put(tp.getId(), tp);
                }
            }
        }
        return tps;
    }

    protected Calendar toCalendar(String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        Calendar calValue = Calendar.getInstance();
        DateTime dt = dateParser.parseDateTime(date);
        calValue.setTime(dt.toDate());
        return calValue;
    }

    @Override
    public TargetPlatformInfo getTargetPlatformInfo(String id) {
        if (id == null) {
            return null;
        }
        TargetPlatformDescriptor desc = platforms.getTargetPlatform(id);
        TargetPlatformInfo tpi = getTargetPlatformInfo(desc);
        return tpi;
    }

    protected TargetPlatformInfo getTargetPlatformInfo(
            TargetPlatformDescriptor desc) {
        if (desc == null) {
            return null;
        }
        String id = desc.getId();
        TargetPlatformInfoImpl tpi = new TargetPlatformInfoImpl(id,
                desc.getName(), desc.getVersion(), desc.getRefVersion(),
                desc.getLabel());
        tpi.setDescription(desc.getDescription());
        tpi.setStatus(desc.getStatus());
        tpi.setEnabled(desc.isEnabled());
        tpi.setReleaseDate(toCalendar(desc.getReleaseDate()));
        tpi.setRestricted(desc.isRestricted());
        tpi.setEndOfAvailability(toCalendar(desc.getEndOfAvailability()));
        tpi.setDownloadLink(desc.getDownloadLink());
        tpi.setDeprecated(desc.isDeprecated());
        tpi.setAvailablePackagesInfo(getTargetPackagesInfo(id));
        return tpi;
    }

    @Override
    public TargetPackage getTargetPackage(String id) {
        if (id == null) {
            return null;
        }
        return getTargetPackage(packages.getTargetPackage(id));
    }

    @Override
    public TargetPackageInfo getTargetPackageInfo(String id) {
        if (id == null) {
            return null;
        }
        TargetPackageDescriptor desc = packages.getTargetPackage(id);
        TargetPackageInfoImpl tpi = new TargetPackageInfoImpl(desc.getId(),
                desc.getName(), desc.getVersion(), desc.getRefVersion(),
                desc.getLabel());
        tpi.setDescription(desc.getDescription());
        tpi.setStatus(desc.getStatus());
        tpi.setEnabled(desc.isEnabled());
        tpi.setReleaseDate(toCalendar(desc.getReleaseDate()));
        tpi.setRestricted(desc.isRestricted());
        tpi.setEndOfAvailability(toCalendar(desc.getEndOfAvailability()));
        tpi.setDownloadLink(desc.getDownloadLink());
        tpi.setDeprecated(desc.isDeprecated());
        tpi.setDependencies(desc.getDependencies());
        return tpi;
    }

    protected TargetPackage getTargetPackage(TargetPackageDescriptor desc) {
        if (desc == null) {
            return null;
        }
        TargetPackageImpl tp = new TargetPackageImpl(desc.getId(),
                desc.getName(), desc.getVersion(), desc.getRefVersion(),
                desc.getLabel());
        tp.setDependencies(desc.getDependencies());
        tp.setDeprecated(desc.isDeprecated());
        tp.setDescription(desc.getDescription());
        tp.setDownloadLink(desc.getDownloadLink());
        tp.setEnabled(desc.isEnabled());
        tp.setEndOfAvailability(toCalendar(desc.getEndOfAvailability()));
        tp.setParent(getTargetPackage(desc.getParent()));
        tp.setRefVersion(desc.getRefVersion());
        tp.setReleaseDate(toCalendar(desc.getReleaseDate()));
        tp.setRestricted(desc.isRestricted());
        tp.setStatus(desc.getStatus());
        tp.setTypes(desc.getTypes());
        return tp;
    }

    @Override
    public TargetPlatformInstance getTargetPlatformInstance(String id,
            List<String> packages) {
        if (id == null) {
            return null;
        }
        TargetPlatformDescriptor desc = platforms.getTargetPlatform(id);
        if (desc == null) {
            return null;
        }
        TargetPlatformInstanceImpl tpi = new TargetPlatformInstanceImpl(id,
                desc.getName(), desc.getVersion(), desc.getRefVersion(),
                desc.getLabel());
        tpi.setDeprecated(desc.isDeprecated());
        tpi.setDescription(desc.getDescription());
        tpi.setDownloadLink(desc.getDownloadLink());
        tpi.setEnabled(desc.isEnabled());
        tpi.setEndOfAvailability(toCalendar(desc.getEndOfAvailability()));
        tpi.setFastTrack(desc.isFastTrack());
        tpi.setParent(getTargetPlatform(desc.getParent()));
        tpi.setRefVersion(desc.getRefVersion());
        tpi.setReleaseDate(toCalendar(desc.getReleaseDate()));
        tpi.setRestricted(desc.isRestricted());
        tpi.setStatus(desc.getStatus());
        tpi.setTypes(desc.getTypes());

        if (packages != null) {
            for (String pkg : packages) {
                TargetPackage tpkg = getTargetPackage(pkg);
                if (tpkg != null) {
                    tpi.addEnabledPackage(tpkg);
                } else {
                    log.warn(String.format(
                            "Referenced target package '%s' not found.", pkg));
                }
            }
        }

        return tpi;
    }

    @Override
    public List<TargetPlatform> getAvailableTargetPlatforms(
            boolean filterDeprecated, boolean filterRestricted, String type) {
        List<TargetPlatform> tps = new ArrayList<>();
        for (TargetPlatformDescriptor desc : platforms.getTargetPlatforms()) {
            if (!desc.isEnabled() || (filterDeprecated && desc.isDeprecated())
                    || (filterRestricted && desc.isRestricted())
                    || (type != null && !desc.matchesType(type))) {
                continue;
            }
            tps.add(getTargetPlatform(desc));
        }
        return tps;
    }

    @Override
    public List<TargetPlatformInfo> getAvailableTargetPlatformsInfo(
            boolean filterDeprecated, boolean filterRestricted, String type) {
        List<TargetPlatformInfo> tps = new ArrayList<>();
        for (TargetPlatformDescriptor desc : platforms.getTargetPlatforms()) {
            if (!desc.isEnabled() || (filterDeprecated && desc.isDeprecated())
                    || (filterRestricted && desc.isRestricted())
                    || (type != null && !desc.matchesType(type))) {
                continue;
            }
            tps.add(getTargetPlatformInfo(desc));
        }
        return tps;
    }

}