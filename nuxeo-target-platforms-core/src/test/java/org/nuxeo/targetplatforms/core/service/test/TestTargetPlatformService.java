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
package org.nuxeo.targetplatforms.core.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hsqldb.jdbcDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SimpleDataSource;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPackageInfo;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;
import org.nuxeo.targetplatforms.api.impl.TargetImpl;
import org.nuxeo.targetplatforms.api.service.TargetPlatformService;
import org.nuxeo.targetplatforms.core.service.DirectoryUpdater;

/**
 * @since 5.7.1
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.ecm.core.schema", "org.nuxeo.ecm.core",
        "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql",
        "org.nuxeo.targetplatforms.core" })
@LocalDeploy("org.nuxeo.targetplatforms.core:OSGI-INF/test-targetplatforms-contrib.xml")
public class TestTargetPlatformService {

    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd",
            Locale.ENGLISH);

    @Inject
    protected TargetPlatformService service;

    @Before
    public void setUpContextFactory() throws NamingException {
        DataSource datasourceAutocommit = new SimpleDataSource(
                "jdbc:hsqldb:mem:memid", jdbcDriver.class.getName(), "SA", "") {
            @Override
            public Connection getConnection() throws SQLException {
                Connection con = super.getConnection();
                con.setAutoCommit(true);
                return con;
            }
        };
        NuxeoContainer.installNaming();
        NuxeoContainer.addDeepBinding("java:comp/env/jdbc/nxsqldirectory",
                datasourceAutocommit);
    }

    @After
    public void tearDown() throws Exception {
        // remove all entries from directory
        new DirectoryUpdater(DirectoryUpdater.DEFAULT_DIR) {
            @Override
            public void run(DirectoryService service, Session session)
                    throws ClientException {
                for (DocumentModel doc : session.getEntries()) {
                    session.deleteEntry(doc.getId());
                }
            }
        }.run();
        NuxeoContainer.uninstallNaming();
    }

    @Test
    public void testService() {
        assertNotNull(service);
    }

    @Test
    public void testGetDefaultTargetPlatform() throws ClientException {
        TargetPlatform tp;
        tp = service.getDefaultTargetPlatform();
        assertNotNull(tp);
        assertEquals("cap-5.8", tp.getId());
    }

    @Test
    public void testGetOverrideDirectory() {
        assertEquals(DirectoryUpdater.DEFAULT_DIR,
                service.getOverrideDirectory());
    }

    @Test
    public void testGetTargetPlatform() throws Exception {
        TargetPlatform tp;
        List<TargetPackage> tps;
        List<String> tpIds;
        List<String> testVersions;

        // check LTS target platform
        tp = service.getTargetPlatform("cap-5.8");
        assertNotNull(tp);
        tps = tp.getAvailablePackages();
        assertEquals(2, tps.size());
        assertEquals("nuxeo-dm-5.8", tps.get(0).getId());
        assertEquals("nuxeo-dam-5.8", tps.get(1).getId());
        tpIds = tp.getAvailablePackagesIds();
        assertEquals(2, tpIds.size());
        assertEquals("nuxeo-dm-5.8", tpIds.get(0));
        assertEquals("nuxeo-dam-5.8", tpIds.get(1));
        assertEquals("This target platform is the last LTS.",
                tp.getDescription());
        assertEquals(
                "http://community.nuxeo.com/static/releases/nuxeo-5.8/nuxeo-cap-5.8-tomcat.zip",
                tp.getDownloadLink());
        assertNull(tp.getEndOfAvailability());
        assertEquals("cap-5.8", tp.getId());
        assertEquals("Nuxeo Platform", tp.getLabel());
        assertEquals("cap", tp.getName());
        assertNull(tp.getParent());
        assertEquals("5.8", tp.getRefVersion());
        assertNotNull(tp.getReleaseDate());
        assertEquals("2013-09-23", format.format(tp.getReleaseDate().getTime()));
        assertEquals("supported", tp.getStatus());
        testVersions = tp.getTestVersions();
        assertEquals(2, testVersions.size());
        assertEquals("5.8.0-HF07-SNAPSHOT", testVersions.get(0));
        assertEquals("5.8", testVersions.get(1));
        assertEquals(0, tp.getTypes().size());
        assertEquals("5.8", tp.getVersion());

        assertFalse(tp.isDeprecated());
        assertTrue(tp.isEnabled());
        assertFalse(tp.isFastTrack());
        assertFalse(tp.isRestricted());

        assertTrue(tp.isAfterVersion(""));
        assertTrue(tp.isAfterVersion(new TargetImpl("")));
        assertTrue(tp.isStrictlyBeforeVersion(""));
        assertTrue(tp.isStrictlyBeforeVersion(new TargetImpl("")));
        assertFalse(tp.isVersion(""));
        assertTrue(tp.isVersion(new TargetImpl("")));

        assertTrue(tp.isAfterVersion("5.6"));
        assertTrue(tp.isAfterVersion(new TargetImpl("nuxeo-cap-5.6", "cap",
                "5.6", null, "Platform 5.6")));
        assertTrue(tp.isStrictlyBeforeVersion(""));
        assertTrue(tp.isStrictlyBeforeVersion(new TargetImpl("")));
        assertTrue(tp.isVersion("5.8"));
        assertFalse(tp.isVersion("5.6"));
        assertTrue(tp.isVersion(new TargetImpl("nuxeo-cap-5.6", "cap", "5.6",
                null, null)));

        // check deprecated target platform
        tp = service.getTargetPlatform("cmf-1.8");
        assertNotNull(tp);
        tps = tp.getAvailablePackages();
        assertEquals(0, tps.size());
        tpIds = tp.getAvailablePackagesIds();
        assertEquals(0, tpIds.size());
        assertEquals(
                "This target platform shows CMF specific features.\n        <p>\n          This is a test description holding HTML tags.\n        </p>",
                tp.getDescription());
        assertEquals(
                "http://community.nuxeo.com/static/releases/cmf-1.8/nuxeo-case-management-distribution-1.8-tomcat-cmf.zip",
                tp.getDownloadLink());
        assertNull(tp.getEndOfAvailability());
        assertEquals("cmf-1.8", tp.getId());
        assertEquals("Nuxeo CMF", tp.getLabel());
        assertEquals("cmf", tp.getName());
        assertNull(tp.getParent());
        assertEquals("5.4.2", tp.getRefVersion());
        assertNull(tp.getReleaseDate());
        assertEquals("deprecated", tp.getStatus());
        testVersions = tp.getTestVersions();
        assertEquals(0, testVersions.size());
        assertEquals(1, tp.getTypes().size());
        assertEquals("CMF", tp.getTypes().get(0));
        assertEquals("1.8", tp.getVersion());

        assertFalse(tp.isDeprecated());
        assertTrue(tp.isEnabled());
        assertFalse(tp.isFastTrack());
        assertFalse(tp.isRestricted());

        // check fast track
        tp = service.getTargetPlatform("cap-5.9.2");
        assertNotNull(tp);
        tps = tp.getAvailablePackages();
        assertEquals(0, tps.size());
        tpIds = tp.getAvailablePackagesIds();
        assertEquals(0, tpIds.size());
        assertNull(tp.getDescription());
        assertEquals(
                "http://community.nuxeo.com/static/releases/nuxeo-5.9.2/nuxeo-cap-5.9.2-tomcat.zip",
                tp.getDownloadLink());
        Calendar date = tp.getEndOfAvailability();
        assertNotNull(date);
        assertEquals("2014-06-18", format.format(date.getTime()));
        assertEquals("cap-5.9.2", tp.getId());
        assertEquals("Nuxeo Platform", tp.getLabel());
        assertEquals("cap", tp.getName());
        TargetPlatform parent = tp.getParent();
        assertEquals("cap-5.9.1", parent.getId());
        assertNotNull(parent);
        assertEquals("cap", parent.getName());
        assertEquals("5.9.1", parent.getVersion());
        assertEquals("5.9.1", parent.getRefVersion());
        assertEquals("5.9.2", tp.getRefVersion());
        assertNotNull(tp.getReleaseDate());
        assertEquals("new", tp.getStatus());
        testVersions = tp.getTestVersions();
        assertEquals(0, testVersions.size());
        assertEquals(0, tp.getTypes().size());
        assertEquals("5.9.2", tp.getVersion());

        assertFalse(tp.isDeprecated());
        assertTrue(tp.isEnabled());
        assertTrue(tp.isFastTrack());
        assertFalse(tp.isRestricted());

        // other use cases
        tp = service.getTargetPlatform("dm-5.3.0");
        assertNotNull(tp);
        assertFalse(tp.isEnabled());

    }

    @Test
    public void testGetTargetPlatformDirOverride() throws ClientException {
        String id = "cap-5.8";
        TargetPlatform tp = service.getTargetPlatform(id);
        assertNotNull(tp);
        assertTrue(tp.isEnabled());
        assertFalse(tp.isRestricted());

        // disable
        service.disableTargetPlatform(id);
        tp = service.getTargetPlatform(id);
        assertFalse(tp.isEnabled());
        assertFalse(tp.isRestricted());

        // enable again
        service.enableTargetPlatform(id);
        tp = service.getTargetPlatform(id);
        assertTrue(tp.isEnabled());
        assertFalse(tp.isRestricted());

        // restrict
        service.restrictTargetPlatform(id);
        tp = service.getTargetPlatform(id);
        assertTrue(tp.isEnabled());
        assertTrue(tp.isRestricted());

        // unrestrict
        service.unrestrictTargetPlatform(id);
        tp = service.getTargetPlatform(id);
        assertTrue(tp.isEnabled());
        assertFalse(tp.isRestricted());

        // disable again
        service.disableTargetPlatform(id);
        tp = service.getTargetPlatform(id);
        assertFalse(tp.isEnabled());
        assertFalse(tp.isRestricted());

        // restore
        service.restoreTargetPlatform(id);
        tp = service.getTargetPlatform(id);
        assertTrue(tp.isEnabled());
        assertFalse(tp.isRestricted());
    }

    @Test
    public void testGetTargetPlatformInfo() throws ClientException {
        TargetPlatformInfo tp = service.getTargetPlatformInfo("cap-5.8");
        assertNotNull(tp);
        List<String> pkgids = tp.getAvailablePackagesIds();
        assertEquals("nuxeo-dm-5.8", pkgids.get(0));
        assertEquals("nuxeo-dam-5.8", pkgids.get(1));
        assertEquals("This target platform is the last LTS.",
                tp.getDescription());
        assertEquals(
                "http://community.nuxeo.com/static/releases/nuxeo-5.8/nuxeo-cap-5.8-tomcat.zip",
                tp.getDownloadLink());
        assertNull(tp.getEndOfAvailability());
        assertEquals("cap-5.8", tp.getId());
        assertEquals("Nuxeo Platform", tp.getLabel());
        assertEquals("cap", tp.getName());
        assertEquals("5.8", tp.getRefVersion());
        assertNotNull(tp.getReleaseDate());
        assertEquals("2013-09-23", format.format(tp.getReleaseDate().getTime()));
        assertEquals("supported", tp.getStatus());
        assertEquals("5.8", tp.getVersion());
        assertFalse(tp.isDeprecated());
        assertTrue(tp.isEnabled());
        assertFalse(tp.isRestricted());
    }

    @Test
    public void testGetTargetPackage() {
        TargetPackage tp;

        tp = service.getTargetPackage("nuxeo-dm-5.8");
        assertNotNull(tp);
        List<String> deps = tp.getDependencies();
        assertEquals(0, deps.size());
        assertEquals("My desc", tp.getDescription());
        assertEquals(
                "https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-dm?version=5.8.0",
                tp.getDownloadLink());
        assertNull(tp.getEndOfAvailability());
        assertEquals("nuxeo-dm-5.8", tp.getId());
        assertEquals("DM", tp.getLabel());
        assertEquals("nuxeo-dm", tp.getName());
        assertNull(tp.getParent());
        assertEquals("5.8", tp.getRefVersion());
        assertNull(tp.getReleaseDate());
        assertEquals("supported", tp.getStatus());
        assertEquals(0, tp.getTypes().size());
        assertEquals("5.8", tp.getVersion());
        assertFalse(tp.isDeprecated());
        assertTrue(tp.isEnabled());
        assertFalse(tp.isRestricted());

        assertTrue(tp.isAfterVersion(""));
        assertTrue(tp.isAfterVersion(new TargetImpl("")));
        assertTrue(tp.isStrictlyBeforeVersion(""));
        assertTrue(tp.isStrictlyBeforeVersion(new TargetImpl("")));
        assertFalse(tp.isVersion(""));
        assertTrue(tp.isVersion(new TargetImpl("")));

        assertTrue(tp.isAfterVersion("5.6"));
        assertFalse(tp.isAfterVersion("5.9.2"));
        assertTrue(tp.isStrictlyBeforeVersion("5.9.2"));
        assertFalse(tp.isStrictlyBeforeVersion("5.6"));
        assertFalse(tp.isStrictlyBeforeVersion("5.8"));
        assertTrue(tp.isVersion("5.8"));
        assertFalse(tp.isVersion("5.9.2"));
    }

    @Test
    public void testGetTargetPackageInfo() {
        TargetPackageInfo tp;

        tp = service.getTargetPackageInfo("nuxeo-dm-5.8");
        assertNotNull(tp);
        List<String> deps = tp.getDependencies();
        assertEquals(0, deps.size());
        assertEquals("My desc", tp.getDescription());
        assertEquals(
                "https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-dm?version=5.8.0",
                tp.getDownloadLink());
        assertNull(tp.getEndOfAvailability());
        assertEquals("nuxeo-dm-5.8", tp.getId());
        assertEquals("DM", tp.getLabel());
        assertEquals("nuxeo-dm", tp.getName());
        assertEquals("5.8", tp.getRefVersion());
        assertNull(tp.getReleaseDate());
        assertEquals("supported", tp.getStatus());
        assertEquals("5.8", tp.getVersion());
        assertFalse(tp.isDeprecated());
        assertTrue(tp.isEnabled());
        assertFalse(tp.isRestricted());
    }

    @Test
    public void testGetTargetPlatformInstance() throws ClientException {
        TargetPlatformInstance tpi;

        tpi = service.getTargetPlatformInstance("cap-5.8", null);
        assertNotNull(tpi);
        assertEquals(0, tpi.getEnabledPackages().size());
        assertEquals(0, tpi.getEnabledPackagesIds().size());
        assertEquals("This target platform is the last LTS.",
                tpi.getDescription());
        assertEquals(
                "http://community.nuxeo.com/static/releases/nuxeo-5.8/nuxeo-cap-5.8-tomcat.zip",
                tpi.getDownloadLink());
        assertNull(tpi.getEndOfAvailability());
        assertEquals("cap-5.8", tpi.getId());
        assertEquals("Nuxeo Platform", tpi.getLabel());
        assertEquals("cap", tpi.getName());
        assertEquals("5.8", tpi.getRefVersion());
        assertNotNull(tpi.getReleaseDate());
        assertEquals("2013-09-23",
                format.format(tpi.getReleaseDate().getTime()));
        assertEquals("supported", tpi.getStatus());
        assertEquals(0, tpi.getTypes().size());
        assertEquals("5.8", tpi.getVersion());
        assertFalse(tpi.hasEnabledPackageWithName("nuxeo-dm-5.8"));

        assertFalse(tpi.isDeprecated());
        assertTrue(tpi.isEnabled());
        assertFalse(tpi.isFastTrack());
        assertFalse(tpi.isRestricted());

        List<String> pkgs = new ArrayList<>();
        pkgs.add("nuxeo-dm-5.8");
        tpi = service.getTargetPlatformInstance("cap-5.8", pkgs);

        assertNotNull(tpi);
        assertEquals(1, tpi.getEnabledPackages().size());
        assertEquals(1, tpi.getEnabledPackagesIds().size());
        assertEquals("This target platform is the last LTS.",
                tpi.getDescription());
        assertEquals(
                "http://community.nuxeo.com/static/releases/nuxeo-5.8/nuxeo-cap-5.8-tomcat.zip",
                tpi.getDownloadLink());
        assertNull(tpi.getEndOfAvailability());
        assertEquals("cap-5.8", tpi.getId());
        assertEquals("Nuxeo Platform", tpi.getLabel());
        assertEquals("cap", tpi.getName());
        assertEquals("5.8", tpi.getRefVersion());
        assertNotNull(tpi.getReleaseDate());
        assertEquals("2013-09-23",
                format.format(tpi.getReleaseDate().getTime()));
        assertEquals("supported", tpi.getStatus());
        assertEquals(0, tpi.getTypes().size());
        assertEquals("5.8", tpi.getVersion());
        assertFalse(tpi.hasEnabledPackageWithName("nuxeo-dm-5.8"));

        assertFalse(tpi.isDeprecated());
        assertTrue(tpi.isEnabled());
        assertFalse(tpi.isFastTrack());
        assertFalse(tpi.isRestricted());
    }

    @Test
    public void testGetAvailableTargetPlatforms() throws ClientException {
        // filter all
        List<TargetPlatform> tps = service.getAvailableTargetPlatforms(true,
                true, null);
        Collections.sort(tps);
        assertEquals(4, tps.size());
        // order is registration order
        assertEquals("cap-5.8", tps.get(0).getId());
        assertEquals("cap-5.9.1", tps.get(1).getId());
        assertEquals("cap-5.9.2", tps.get(2).getId());
        assertEquals("cmf-1.8", tps.get(3).getId());

        // filter deprecated
        tps = service.getAvailableTargetPlatforms(true, true, null);
        Collections.sort(tps);
        assertEquals(4, tps.size());
        // order is registration order
        assertEquals("cap-5.8", tps.get(0).getId());
        assertEquals("cap-5.9.1", tps.get(1).getId());
        assertEquals("cap-5.9.2", tps.get(2).getId());
        assertEquals("cmf-1.8", tps.get(3).getId());

        // filter restricted
        tps = service.getAvailableTargetPlatforms(true, false, null);
        Collections.sort(tps);
        assertEquals(5, tps.size());
        assertEquals("cap-5.8", tps.get(0).getId());
        assertEquals("cap-5.9.1", tps.get(1).getId());
        assertEquals("cap-5.9.2", tps.get(2).getId());
        assertEquals("cap-5.9.3", tps.get(3).getId());
        assertEquals("cmf-1.8", tps.get(4).getId());

        // filter on type
        tps = service.getAvailableTargetPlatforms(false, false, "CMF");
        Collections.sort(tps);
        assertEquals(1, tps.size());
        assertEquals("cmf-1.8", tps.get(0).getId());
    }

    @Test
    public void testGetAvailableTargetPlatformsOverride()
            throws ClientException {
        List<TargetPlatform> tps = service.getAvailableTargetPlatforms(true,
                true, null);
        Collections.sort(tps);
        assertEquals(4, tps.size());
        assertEquals("cap-5.8", tps.get(0).getId());
        assertEquals("cap-5.9.1", tps.get(1).getId());
        assertEquals("cap-5.9.2", tps.get(2).getId());
        assertEquals("cmf-1.8", tps.get(3).getId());

        service.restrictTargetPlatform("cap-5.9.2");

        tps = service.getAvailableTargetPlatforms(true, true, null);
        Collections.sort(tps);
        assertEquals(3, tps.size());
        assertEquals("cap-5.8", tps.get(0).getId());
        assertEquals("cap-5.9.1", tps.get(1).getId());
        assertEquals("cmf-1.8", tps.get(2).getId());
    }

    @Test
    public void testGetAvailableTargetPlatformsInfo() throws ClientException {
        // filter all
        List<TargetPlatformInfo> tps = service.getAvailableTargetPlatformsInfo(
                true, true, null);
        Collections.sort(tps);
        assertEquals(4, tps.size());
        assertEquals("cap-5.8", tps.get(0).getId());
        assertEquals("cap-5.9.1", tps.get(1).getId());
        assertEquals("cap-5.9.2", tps.get(2).getId());
        assertEquals("cmf-1.8", tps.get(3).getId());

        // filter deprecated
        tps = service.getAvailableTargetPlatformsInfo(true, false, null);
        Collections.sort(tps);
        assertEquals(5, tps.size());
        assertEquals("cap-5.8", tps.get(0).getId());
        assertEquals("cap-5.9.1", tps.get(1).getId());
        assertEquals("cap-5.9.2", tps.get(2).getId());
        assertEquals("cap-5.9.3", tps.get(3).getId());
        assertEquals("cmf-1.8", tps.get(4).getId());

        // filter restricted
        tps = service.getAvailableTargetPlatformsInfo(true, false, null);
        Collections.sort(tps);
        assertEquals(5, tps.size());
        assertEquals("cap-5.8", tps.get(0).getId());
        assertEquals("cap-5.9.1", tps.get(1).getId());
        assertEquals("cap-5.9.2", tps.get(2).getId());
        assertEquals("cap-5.9.3", tps.get(3).getId());
        assertEquals("cmf-1.8", tps.get(4).getId());

        // filter on type
        tps = service.getAvailableTargetPlatformsInfo(false, false, "CMF");
        Collections.sort(tps);
        assertEquals(1, tps.size());
        assertEquals("cmf-1.8", tps.get(0).getId());

        // disable target platform
        service.disableTargetPlatform("cmf-1.8");
        tps = service.getAvailableTargetPlatformsInfo(false, false, "CMF");
        assertEquals(0, tps.size());
    }

}