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

package com.redhat.ceylon.ceylondoc;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.redhat.ceylon.compiler.typechecker.model.ClassOrInterface;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.Module;
import com.redhat.ceylon.compiler.typechecker.model.Package;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.model.Scope;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.TypeParameter;
import com.redhat.ceylon.compiler.typechecker.model.UnionType;
import com.redhat.ceylon.compiler.typechecker.model.Unit;

public abstract class CeylonDoc extends Markup {

    protected final CeylonDocTool tool;
    protected final Module module;

    public CeylonDoc(Module module, CeylonDocTool tool, File file) {
        super(file);
        this.module = module;
        this.tool = tool;
    }
    
    // FIXME: copied from ProducedType, we should make it public
    private static boolean isElementOfUnion(UnionType ut, TypeDeclaration td) {
        for (TypeDeclaration ct: ut.getCaseTypeDeclarations()) {
            if (ct.equals(td)) return true;
        }
        return false;
    }

    protected void link(ProducedType type) throws IOException {
        TypeDeclaration decl = type.getDeclaration();
        if(decl instanceof UnionType){
            UnionType ut = (UnionType) decl;
            // try to simplify if possible
            if (ut.getCaseTypes().size()==2) {
                Unit unit = decl.getUnit();
                if (isElementOfUnion(ut, unit.getNothingDeclaration())) {
                    link(unit.getDefiniteType(type));
                    write("?");
                    return;
                }
                if (isElementOfUnion(ut, unit.getEmptyDeclaration()) &&
                        isElementOfUnion(ut, unit.getSequenceDeclaration())) {
                    link(unit.getElementType(type));
                    write("[]");
                    return;
                }
            }
            // simplification failed, do it the hard way
            boolean first = true;
            for(ProducedType ud : ut.getCaseTypes()){
                if(first){
                    first = false;
                }else{
                    write("|");
                }
                link(ud);
            }
        }else if(decl instanceof ClassOrInterface){
            link((ClassOrInterface) decl, type.getTypeArgumentList());
        } else if (decl instanceof TypeParameter) {
            around("span class='type-parameter'", decl.getName());
        } else {
            write(type.getProducedTypeName());
        }
    }

    protected void link(ClassOrInterface decl, List<ProducedType> typeParameters) throws IOException {
        String name = decl.getName();
        around("a href='" + getObjectUrl(decl) + "'", name);
        if (typeParameters != null && !typeParameters.isEmpty()) {
            write("&lt;");
            boolean once = false;
            for (ProducedType typeParam : typeParameters) {
                if (!once)
                    once = true;
                else
                    write(",");
                link(typeParam);
            }
            write("&gt;");
        }
    }

    protected void linkToMember(Declaration decl) throws IOException {
        ClassOrInterface container = (ClassOrInterface) decl.getContainer();
        String name = decl.getName();
        around("a href='" + getObjectUrl(container) + "#"+ name + "'", name);
    }

    protected String getFileName(Scope klass) {
        List<String> name = new LinkedList<String>();
        while (klass instanceof Declaration) {
            name.add(0, ((Declaration) klass).getName());
            klass = klass.getContainer();
        }
        return join(".", name) + ".html";
    }

    protected File getFolder(Package pkg) {
        File dir = new File(tool.getDestDir(), join("/", pkg.getName()));
        dir.mkdirs();
        return dir;
    }

    protected File getFolder(ClassOrInterface klass) {
        return getFolder(getPackage(klass));
    }

    protected static Package getPackage(Scope decl) {
        while (!(decl instanceof Package)) {
            decl = decl.getContainer();
        }
        return (Package) decl;
    }

    protected static String join(String str, List<String> parts) {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> iterator = parts.iterator();
        while (iterator.hasNext()) {
            stringBuilder.append(iterator.next());
            if (iterator.hasNext())
                stringBuilder.append(str);
        }
        return stringBuilder.toString();
    }

    protected boolean include(Declaration decl){
        return tool.include(decl);
    }
    
    protected abstract String getObjectUrl(Object to) throws IOException;
    
    protected abstract String getResourceUrl(String to) throws IOException;
    
    protected abstract String getSrcUrl(Object to) throws IOException;


}


