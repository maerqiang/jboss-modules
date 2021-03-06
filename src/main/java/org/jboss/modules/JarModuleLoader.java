/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class JarModuleLoader extends ModuleLoader {

    static final String[] NO_STRINGS = new String[0];
    private final ModuleLoader delegate;
    private final JarFile jarFile;
    private final ModuleIdentifier myIdentifier;

    JarModuleLoader(final ModuleLoader delegate, final JarFile jarFile) {
        super(new ModuleFinder[] { new JarModuleFinder(simpleNameOf(jarFile), jarFile) });
        this.delegate = delegate;
        this.jarFile = jarFile;
        myIdentifier = simpleNameOf(jarFile);
    }

    private static ModuleIdentifier simpleNameOf(JarFile jarFile) {
        String jarName = jarFile.getName();
        String simpleJarName = jarName.substring(jarName.lastIndexOf(File.separatorChar) + 1);
        return ModuleIdentifier.create(simpleJarName);
    }

    protected Module preloadModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        if (identifier.equals(myIdentifier)) {
            return loadModuleLocal(identifier);
        } else {
            Module module = loadModuleLocal(identifier);
            if (module == null) {
                return preloadModule(identifier, delegate);
            } else {
                return module;
            }
        }
    }

    ModuleIdentifier getMyIdentifier() {
        return myIdentifier;
    }

    public String toString() {
        return "JAR module loader";
    }

    static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException e) {
            // ignore
        }
    }
}
