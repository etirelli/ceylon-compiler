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

import java.util.List;

import com.redhat.ceylon.compiler.typechecker.model.Annotation;
import com.redhat.ceylon.compiler.typechecker.model.Class;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.Parameter;
import com.redhat.ceylon.compiler.typechecker.model.ParameterList;
import com.redhat.ceylon.compiler.typechecker.model.ProducedReference;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.model.Scope;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.TypeParameter;
import com.redhat.ceylon.compiler.typechecker.model.Unit;
import com.redhat.ceylon.compiler.util.Util;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.ClassSymbol;

public class LazyClass extends Class implements LazyElement {

    public ClassSymbol classSymbol;
    private ModelCompleter completer;
    private boolean isLoaded = false;
    private boolean isTypeParamsLoaded = false;
    private boolean forTopLevelObject;

    public LazyClass(ClassSymbol classSymbol, ModelCompleter completer, boolean forTopLevelObject) {
        this.classSymbol = classSymbol;
        this.completer = completer;
        this.forTopLevelObject = forTopLevelObject;
        // FIXME Using flatName() instead of just getSimpleName() is for anonymous classes, but do we need them?
        setName(Util.getSimpleName(classSymbol.flatName().toString()));
        setAbstract((classSymbol.flags() & Flags.ABSTRACT) != 0);
    }
    
    public boolean isTopLevelObjectType(){
        return forTopLevelObject;
    }
    
    private void load() {
        if(!isLoaded){
            loadTypeParams();
            isLoaded = true;
            completer.complete(this);
        }
    }

    private void loadTypeParams() {
        if(!isTypeParamsLoaded){
            isTypeParamsLoaded = true;
            completer.completeTypeParameters(this);
        }
    }
    
    @Override
    public String toString() {
        if (!isLoaded) {
            return "UNLOADED:" + super.toString();
        }
        return super.toString();
    }

    @Override
    public ParameterList getParameterList() {
        load();
        return super.getParameterList();
    }

    @Override
    public Parameter getParameter(String name) {
        load();
        return super.getParameter(name);
    }

    @Override
    public List<Declaration> getMembers() {
        load();
        return super.getMembers();
    }

    @Override
    public ProducedType getType() {
        loadTypeParams();
        return super.getType();
    }

    @Override
    public ProducedType getExtendedType() {
        load();
        return super.getExtendedType();
    }
    
    @Override
    public List<TypeParameter> getTypeParameters() {
        loadTypeParams();
        return super.getTypeParameters();
    }
    
    @Override
    public List<ParameterList> getParameterLists() {
        load();
        return super.getParameterLists();
    }

    @Override
    public boolean isMember() {
        return super.isMember();
    }

    @Override
    public ProducedType getDeclaringType(Declaration d) {
        load();
        return super.getDeclaringType(d);
    }

    @Override
    public boolean isParameterized() {
        load();
        return super.isParameterized();
    }

    @Override
    public Class getExtendedTypeDeclaration() {
        load();
        return super.getExtendedTypeDeclaration();
    }

    @Override
    public List<TypeDeclaration> getSatisfiedTypeDeclarations() {
        load();
        return super.getSatisfiedTypeDeclarations();
    }

    @Override
    public List<ProducedType> getSatisfiedTypes() {
        load();
        return super.getSatisfiedTypes();
    }

    @Override
    public List<TypeDeclaration> getCaseTypeDeclarations() {
        load();
        return super.getCaseTypeDeclarations();
    }

    @Override
    public List<ProducedType> getCaseTypes() {
        load();
        return super.getCaseTypes();
    }

    @Override
    public ProducedReference getProducedReference(ProducedType pt,
            List<ProducedType> typeArguments) {
        loadTypeParams();
        return super.getProducedReference(pt, typeArguments);
    }

    @Override
    public ProducedType getProducedType(ProducedType outerType,
            List<ProducedType> typeArguments) {
        loadTypeParams();
        return super.getProducedType(outerType, typeArguments);
    }

    @Override
    public List<Declaration> getInheritedMembers(String name) {
        load();
        return super.getInheritedMembers(name);
    }

    @Override
    public Declaration getRefinedMember(String name) {
        load();
        return super.getRefinedMember(name);
    }
    
    @Override
    public Declaration getMember(String name) {
        load();
        return super.getMember(name);
    }

    @Override
    public Declaration getMemberOrParameter(String name) {
        load();
        return super.getMemberOrParameter(name);
    }

    @Override
    public boolean isAlias() {
        load();
        return super.isAlias();
    }

    @Override
    public ProducedType getSelfType() {
        load();
        return super.getSelfType();
    }

    @Override
    public Scope getVisibleScope() {
        load();
        return super.getVisibleScope();
    }

    @Override
    public boolean isShared() {
        load();
        return super.isShared();
    }

    @Override
    public List<Annotation> getAnnotations() {
        load();
        return super.getAnnotations();
    }

    @Override
    public String getQualifiedNameString() {
        load();
        return super.getQualifiedNameString();
    }

    @Override
    public boolean isActual() {
        load();
        return super.isActual();
    }

    @Override
    public boolean isFormal() {
        load();
        return super.isFormal();
    }

    @Override
    public boolean isDefault() {
        load();
        return super.isDefault();
    }

    @Override
    public boolean isVisible(Scope scope) {
        load();
        return super.isVisible(scope);
    }

    @Override
    public boolean isDefinedInScope(Scope scope) {
        load();
        return super.isDefinedInScope(scope);
    }

    @Override
    public boolean isCaptured() {
        load();
        return super.isCaptured();
    }

    @Override
    public boolean isToplevel() {
        load();
        return super.isToplevel();
    }

    @Override
    public boolean isClassMember() {
        load();
        return super.isClassMember();
    }

    @Override
    public boolean isInterfaceMember() {
        load();
        return super.isInterfaceMember();
    }

    @Override
    public boolean isClassOrInterfaceMember() {
        load();
        return super.isClassOrInterfaceMember();
    }

    @Override
    public Unit getUnit() {
        load();
        return super.getUnit();
    }

    @Override
    public Scope getContainer() {
        return super.getContainer();
    }

    @Override
    public Declaration getDirectMemberOrParameter(String name) {
        load();
        return super.getDirectMemberOrParameter(name);
    }

    @Override
    public Declaration getDirectMember(String name) {
        load();
        return super.getDirectMember(name);
    }

    @Override
    protected boolean isParameter(Declaration d) {
        load();
        return super.isParameter(d);
    }

    @Override
    public Declaration getMemberOrParameter(Unit unit, String name) {
        load();
        return super.getMemberOrParameter(unit, name);
    }

    @Override
    public void addMember(Declaration decl) {
        // do this without lazy-loading
        super.getMembers().add(decl);
    }
}
