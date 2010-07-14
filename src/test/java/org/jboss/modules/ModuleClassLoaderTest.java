/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.modules;

import org.jboss.modules.test.ImportedClass;
import org.jboss.modules.test.TestClass;
import org.jboss.modules.util.TestModuleLoader;
import org.jboss.modules.util.TestResourceLoader;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import static org.jboss.modules.util.Util.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test to verify module functionality.
 *
 * @author John Bailey
 */
public class ModuleClassLoaderTest extends AbstractModuleTestCase {

    private static final ModuleIdentifier MODULE_WITH_CONTENT_ID = new ModuleIdentifier("test", "test-with-content", "1.0");
    private static final ModuleIdentifier MODULE_TO_IMPORT_ID = new ModuleIdentifier("test", "test-to-import", "1.0");
    private static final ModuleIdentifier MODULE_WITH_EXPORT_ID = new ModuleIdentifier("test", "test-with-export", "1.0");
    private static final ModuleIdentifier MODULE_WITH_FILTERED_EXPORT_ID = new ModuleIdentifier("test", "test-with-filtered-export", "1.0");

    private TestModuleLoader moduleLoader;

    @Before
    public void setupModuleLoader() throws Exception {
        moduleLoader = new TestModuleLoader();

        final ModuleSpec.Builder moduleWithContentBuilder = ModuleSpec.build(MODULE_WITH_CONTENT_ID);
        moduleWithContentBuilder.addRoot("rootOne",
            TestResourceLoader.build()
                .addClass(TestClass.class)
                .addResources(getResource("test/modulecontentloader/rootOne"))
                .create()
        );
        moduleWithContentBuilder.addDependency(MODULE_TO_IMPORT_ID);
        moduleLoader.addModuleSpec(moduleWithContentBuilder.create());

        final ModuleSpec.Builder moduleToImportBuilder = ModuleSpec.build(MODULE_TO_IMPORT_ID);
        moduleToImportBuilder.addRoot("rootOne",
            TestResourceLoader.build()
                .addClass(ImportedClass.class)
                .addResources(getResource("test/modulecontentloader/rootTwo"))
                .create()
        );
        moduleLoader.addModuleSpec(moduleToImportBuilder.create());

        final ModuleSpec.Builder moduleWithExportBuilder = ModuleSpec.build(MODULE_WITH_EXPORT_ID);
        moduleWithExportBuilder
            .addDependency(MODULE_TO_IMPORT_ID).setExport(true);
        moduleLoader.addModuleSpec(moduleWithExportBuilder.create());

        final ModuleSpec.Builder moduleWithExportFilterBuilder = ModuleSpec.build(MODULE_WITH_FILTERED_EXPORT_ID);
        moduleWithExportFilterBuilder
            .addDependency(MODULE_TO_IMPORT_ID)
                .setExport(true)
                .addExportExclude("org/jboss/**")
                .addExportExclude("nested");

        moduleLoader.addModuleSpec(moduleWithExportFilterBuilder.create());
    }

    @Test
    public void testLocalClassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            Class<?> testClass = classLoader.loadClass("org.jboss.modules.test.TestClass");
            assertNotNull(testClass);
        } catch (ClassNotFoundException e) {
            fail("Should have loaded local class");
        }
    }

    @Test
    public void testLocalClassLoadNotFound() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            Class<?> testClass = classLoader.loadClass("org.jboss.modules.test.BogusClass");
            fail("Should have thrown ClassNotFoundException");
        } catch (ClassNotFoundException expected) {
        }
    }

    @Test
    public void testImportClassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            Class<?> testClass = classLoader.loadClass("org.jboss.modules.test.ImportedClass");
            assertNotNull(testClass);
        } catch (ClassNotFoundException e) {
            fail("Should have loaded imported class");
        }
    }

    @Test
    public void testExportClassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            classLoader.loadExportedClass("org.jboss.modules.test.ImportedClass");
            fail("Should have thrown ClassNotFoundException");
        } catch (ClassNotFoundException expected) {
        }

        final Module exportingModule = moduleLoader.loadModule(MODULE_WITH_EXPORT_ID);
        final ModuleClassLoader exportingClassLoader = exportingModule.getClassLoader();

        try {
            Class<?> testClass = exportingClassLoader.loadExportedClass("org.jboss.modules.test.ImportedClass");
            assertNotNull(testClass);
        } catch (ClassNotFoundException e) {
            fail("Should have loaded exported class");
        }
    }

    @Test
    public void testFilteredExportClassLoad() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_FILTERED_EXPORT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        try {
            classLoader.loadExportedClass("org.jboss.modules.test.ImportedClass");
            fail("Should have thrown ClassNotFoundException");
        } catch (ClassNotFoundException expected) {
        }
    }

    @Test
    public void testLocalResourceRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final URL resUrl = classLoader.getResource("test.txt");
        assertNotNull(resUrl);
    }

    @Test
    public void testLocalResourceRetrievalNotFound() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final URL resUrl = classLoader.getResource("bogus.txt");
        assertNull(resUrl);
    }

    @Test
    public void testImportResourceRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final URL resUrl = classLoader.getResource("testTwo.txt");
        assertNotNull(resUrl);
    }

    @Test
    public void testExportResourceRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        URL resUrl = classLoader.getExportedResource("testTwo.txt");
        assertNull(resUrl);

        final Module exportingModule = moduleLoader.loadModule(MODULE_WITH_EXPORT_ID);
        final ModuleClassLoader exportingClassLoader = exportingModule.getClassLoader();

        resUrl = exportingClassLoader.getExportedResource("testTwo.txt");
        assertNotNull(resUrl);
    }

    @Test
    public void testFilteredExportResourceRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_FILTERED_EXPORT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        URL resUrl = classLoader.getResource("nested/nested.txt");
        assertNotNull(resUrl);

        resUrl = classLoader.getExportedResource("nested/nested.txt");
        assertNull(resUrl);
    }

    @Test
    public void testLocalResourcesRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final Enumeration<URL> resUrls = classLoader.getResources("test.txt");
        assertNotNull(resUrls);
        final List<URL> resUrlList = toList(resUrls);
        assertEquals(1, resUrlList.size());
        assertTrue(resUrlList.get(0).getPath().contains("rootOne"));
    }

    @Test
    public void testLocalResourcesRetrievalNotFound() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final Enumeration<URL> resUrls = classLoader.getResources("bogus.txt");
        assertNotNull(resUrls);
        final List<URL> resUrlList = toList(resUrls);
        assertTrue(resUrlList.isEmpty());
    }

    @Test
    public void testImportResourcesRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final Enumeration<URL> resUrls = classLoader.getResources("testTwo.txt");
        assertNotNull(resUrls);
        final List<URL> resUrlList = toList(resUrls);
        assertEquals(1, resUrlList.size());
        assertTrue(resUrlList.get(0).getPath().contains("rootTwo"));
    }

    @Test
    public void testLocalAndImportResourcesRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();
        final Enumeration<URL> resUrls = classLoader.getResources("nested/nested.txt");
        assertNotNull(resUrls);
        final List<URL> resUrlList = toList(resUrls);
        assertEquals(2, resUrlList.size());
        assertTrue(resUrlList.get(0).getPath().contains("rootTwo"));
        assertTrue(resUrlList.get(1).getPath().contains("rootOne"));
    }

    @Test
    public void testExportResourcesRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_CONTENT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        Enumeration<URL> resUrls = classLoader.getExportedResources("testTwo.txt");
        List<URL> resUrlList = toList(resUrls);
        assertTrue(resUrlList.isEmpty());

        final Module exportingModule = moduleLoader.loadModule(MODULE_WITH_EXPORT_ID);
        final ModuleClassLoader exportingClassLoader = exportingModule.getClassLoader();

        resUrls = exportingClassLoader.getExportedResources("testTwo.txt");
        resUrlList = toList(resUrls);
        assertEquals(1, resUrlList.size());
        assertTrue(resUrlList.get(0).getPath().contains("rootTwo"));
    }

    @Test
    public void testFilteredExportsResourceRetrieval() throws Exception {
        final Module testModule = moduleLoader.loadModule(MODULE_WITH_FILTERED_EXPORT_ID);
        final ModuleClassLoader classLoader = testModule.getClassLoader();

        Enumeration<URL> resUrls = classLoader.getResources("nested/nested.txt");
        List<URL> resUrlList = toList(resUrls);
        assertFalse(resUrlList.isEmpty());

        resUrls = classLoader.getExportedResources("nested/nested.txt");
        resUrlList = toList(resUrls);
        assertTrue(resUrlList.isEmpty());
    }
}
