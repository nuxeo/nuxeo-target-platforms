/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.core.service.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.service.TargetPlatformService;


/**
 * @since 2.18
 */
public class TestTargetPlatformComponent extends NXRuntimeTestCase {

    protected TargetPlatformService service;

    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.targetplatforms.core");

        service = Framework.getService(TargetPlatformService.class);
        assertNotNull(service);

        String contrib = "OSGI-INF/test-targetplatforms-contrib.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        deployTestContrib("org.nuxeo.targetplatforms.core", url);
    }

    @Test
    public void testPlatformRegistration() {
        TargetPlatform tp = service.getTargetPlatform("cap-5.8");
        assertNotNull(tp);
        assertTrue(tp.isEnabled());
    }

    @Test
    public void testPackageRegistration() {
        TargetPackage tp = service.getTargetPackage("nuxeo-dm-5.8");
        assertNotNull(tp);
        assertTrue(tp.isEnabled());
    }

    @Test
    public void testPlatformRegistrationOverride() throws Exception {
        TargetPlatform tpOld = service.getTargetPlatform("dm-5.3.0");
        assertNotNull(tpOld);
        assertFalse(tpOld.isEnabled());

        TargetPlatform tpNew = service.getTargetPlatform("cap-5.9.2");
        assertNotNull(tpNew);
        assertTrue(tpNew.isEnabled());

        String contrib = "OSGI-INF/test-targetplatforms-override-contrib.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        deployTestContrib("org.nuxeo.targetplatforms.core", url);

        tpOld = service.getTargetPlatform("dm-5.3.0");
        assertNotNull(tpOld);
        assertTrue(tpOld.isEnabled());

        tpNew = service.getTargetPlatform("cap-5.9.2");
        assertNotNull(tpNew);
        assertFalse(tpNew.isEnabled());
    }

    @Test
    public void testPackageRegistrationOverride() throws Exception {
        TargetPackage tp = service.getTargetPackage("nuxeo-dm-5.8");
        assertNotNull(tp);
        assertTrue(tp.isEnabled());

        String contrib = "OSGI-INF/test-targetplatforms-override-contrib.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        deployTestContrib("org.nuxeo.targetplatforms.core", url);

        tp = service.getTargetPackage("nuxeo-dm-5.8");
        assertNotNull(tp);
        assertFalse(tp.isEnabled());
    }
}