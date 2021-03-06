/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package com.redhat.ceylon.compiler.loader;

import com.redhat.ceylon.compiler.typechecker.model.Module;
import com.redhat.ceylon.compiler.typechecker.model.Package;
import com.sun.tools.javac.util.Context;

public class CompilerModule extends Module {

    private Context context;
    private CeylonModelLoader modelLoader;

    public CompilerModule(com.sun.tools.javac.util.Context context) {
        this.context = context;
    }

    public CompilerModule(CeylonModelLoader modelLoader) {
        this.modelLoader = modelLoader;
    }

    @Override
    public Package getPackage(String name) {
        System.err.println("Looking up package "+name);
        Package pkg = super.getPackage(name);
        if(pkg == null)
            pkg = getModelLoader().findOrCreatePackage(this, name);
        return pkg;
    }

    private CeylonModelLoader getModelLoader() {
        if(modelLoader == null){
            modelLoader = CeylonModelLoader.instance(context);
        }
        return modelLoader;
    }

}
