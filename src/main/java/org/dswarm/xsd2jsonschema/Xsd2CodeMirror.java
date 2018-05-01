/**
 * Copyright (C) 2013 – 2016 SLUB Dresden & Avantgarde Labs GmbH (<code@dswarm.org>)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dswarm.xsd2jsonschema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.sun.org.apache.xerces.internal.xs.XSAttributeDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSAttributeUse;
import com.sun.org.apache.xerces.internal.xs.XSComplexTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSElementDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSImplementation;
import com.sun.org.apache.xerces.internal.xs.XSLoader;
import com.sun.org.apache.xerces.internal.xs.XSModel;
import com.sun.org.apache.xerces.internal.xs.XSModelGroup;
import com.sun.org.apache.xerces.internal.xs.XSNamedMap;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSParticle;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSTerm;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSWildcard;
import org.dswarm.xsd2jsonschema.SimpleElements.CmArray;
import org.dswarm.xsd2jsonschema.SimpleElements.CmElement;
import org.dswarm.xsd2jsonschema.SimpleElements.CmObject;
import org.dswarm.xsd2jsonschema.SimpleElements.CmString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.SAXException;

public class Xsd2CodeMirror {

    private static final String HASH = "#";
    private static final String HTTP_PREFIX = "http://";
    private static final String SLASH = "/";
    private static final String AT = "@";
    private static final String WILDCARD = "wildcard";
    private static final String NULL = "null";

    private static final Logger LOG = LoggerFactory.getLogger(Xsd2CodeMirror.class);

    private static final XSLoader LOADER;
    private static final int MAX_RECURSION_DEPTH = 7;

    private int depth = 0;
    private Map<String, Optional<CmElement>> elements = new HashMap<>();

    static {
        System.setProperty(DOMImplementationRegistry.PROPERTY, "com.sun.org.apache.xerces.internal.dom.DOMXSImplementationSourceImpl");

        XSLoader loader;
        try {
            final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            final XSImplementation impl = (XSImplementation) registry.getDOMImplementation("XS-Loader");
            loader = impl.createXSLoader(null);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new UnsupportedOperationException("Not yet implemented");
        }
        LOADER = loader;
    }

    private XSModel model;

    private Collection<CmElement> iterateParticle(final XSParticle particle) {

        final XSTerm term = particle.getTerm();

        if (term instanceof XSModelGroup) {

            final XSModelGroup modelGroup = (XSModelGroup) term;
            return iterateModelGroup(modelGroup);
        } else {
            throw new UnsupportedOperationException("Non XSModelGroup not yet implemented");
        }
    }

    private Optional<CmElement> iterateSingleParticle(final XSParticle particle) {

        final XSTerm term = particle.getTerm();
        if (term instanceof XSElementDeclaration) {

            final XSElementDeclaration xsElementDecl = (XSElementDeclaration) term;

            final Optional<CmElement> optionalElement = iterateElement(xsElementDecl);

            return optionalElement.map(el -> isRepeated(particle) ? new CmArray(el) : el);
        } else if (term instanceof XSWildcard) {

            final XSWildcard wildcard = (XSWildcard) term;

            // TODO: what should we do with other XS wildcard types, i.e., 'any' and 'union'

            if (XSWildcard.NSCONSTRAINT_ANY == wildcard.getConstraintType()) {

                // TODO: shall we do something else here??

                return Optional.empty();
            }
        }

        throw new UnsupportedOperationException("Non XSElementDeclaration or XSWildcard terms not yet implemented");
    }

    private List<CmElement> iterateModelGroup(final XSModelGroup modelGroup) {

        final List<CmElement> list = new ArrayList<>();

        final XSObjectList particles = modelGroup.getParticles();
        depth++;
        for (int i = 0, l = particles.getLength(); i < l; i++) {

            final XSParticle xsParticle = (XSParticle) particles.item(i);
            final XSTerm term = xsParticle.getTerm();
            if (term instanceof XSModelGroup) {
                list.addAll(iterateParticle(xsParticle));
            }  else {

                final Optional<CmElement> optionalCmElement = iterateSingleParticle(xsParticle);
                optionalCmElement.ifPresent(list::add);
            }
        }
        depth--;
        return list;
    }

    private Optional<CmElement> iterateElement(final XSElementDeclaration elementDecl) {
        if (depth > MAX_RECURSION_DEPTH) {
            LOG.error("Structure deeper than ", MAX_RECURSION_DEPTH);
            return Optional.empty();
        }

            final String elementName = getDeclarationName(elementDecl);

            if (!elements.containsKey(elementName)) {
                final Optional<CmElement> CmElement = getCmElement(elementDecl, elementName);
                elements.put(elementName, CmElement);
            }
            return elements.get(elementName);

    }

    private Optional<CmElement> getCmElement(XSElementDeclaration elementDecl, String elementName) {
        final XSTypeDefinition xsElementDeclType = elementDecl.getTypeDefinition();

        if (XSTypeDefinition.SIMPLE_TYPE == xsElementDeclType.getTypeCategory()) {
            throw new UnsupportedOperationException("Not yet implemented");

        } else if (XSTypeDefinition.COMPLEX_TYPE == xsElementDeclType.getTypeCategory()) {

            final XSComplexTypeDefinition xsComplexType = (XSComplexTypeDefinition) xsElementDeclType;
            final boolean isMixed = isMixed(xsComplexType);

            final CmElement element = Optional.ofNullable(xsComplexType.getSimpleType()).map(type -> {

                final CmElement simpleCmElement = iterateSimpleType(type).withName(elementName);

                final int numAttributes = xsComplexType.getAttributeUses().size();
                if (numAttributes <= 0) {
                    throw new UnsupportedOperationException("Not yet implemented");

                }

                final Collection<CmElement> elements = new ArrayList<>(numAttributes);
                final CmObject CmElements;

                // to avoid doubling of attribute in attribute path
                if (!elementName.equals(simpleCmElement.getName())) {
                    throw new UnsupportedOperationException("Not yet implemented");

                } else {

                    CmElements = new CmObject(elementName, true);
                }

                iterateComplexAttributes(xsComplexType, elements);

                CmElements.addAll(elements);

                return CmElements;
            }).orElseGet(() -> {
                final CmObject CmElements = new CmObject(elementName, isMixed);

                final List<CmElement> elements = iterateComplexType(xsComplexType);

                CmElements.addAll(elements);

                return CmElements;
            });

            return Optional.of(element);
        }

        throw new UnsupportedOperationException("Not yet implemented");

    }

    private List<CmElement> iterateComplexType(final XSComplexTypeDefinition complexType) {

        final List<CmElement> result = new ArrayList<>();

        final XSParticle xsParticle = complexType.getParticle();
        if (xsParticle != null) {

            result.addAll(iterateParticle(xsParticle));
        } else {
            final XSSimpleTypeDefinition xsSimpleType = complexType.getSimpleType();
            if (xsSimpleType != null) {
                throw new UnsupportedOperationException("Not yet implemented");
            }
        }

        iterateComplexAttributes(complexType, result);

        return result;
    }

    private static void iterateComplexAttributes(final XSComplexTypeDefinition complexType, final Collection<CmElement> result) {

        final XSObjectList attributeUses = complexType.getAttributeUses();

        for (int i = 0; i < attributeUses.getLength(); i++) {
            final XSAttributeDeclaration attributeUseDecl = ((XSAttributeUse) attributeUses.item(i)).getAttrDeclaration();
            final XSSimpleTypeDefinition type = attributeUseDecl.getTypeDefinition();

            final String attributeName = getDeclarationName(attributeUseDecl, complexType);

            result.add(iterateSimpleType(type).withName(AT + attributeName));
        }
    }

    private static CmElement iterateSimpleType(final XSObject simpleType) {

        final String simpleTypeName = getDeclarationName(simpleType);

        return new CmString(simpleTypeName);
    }

    public void parse(final LSInput input) {
        model = LOADER.load(input);
    }


    public void apply() throws SAXException {

        Preconditions.checkState(model != null, "You have to call parse() first");

        final XSNamedMap elements = model.getComponents(XSConstants.ELEMENT_DECLARATION);
        for (int i = 0; i < elements.getLength(); i++) {

            final XSObject object = elements.item(i);

            if (object instanceof XSElementDeclaration) {

                final XSElementDeclaration elementDecl = (XSElementDeclaration) object;

                if (elementDecl.getAbstract()) {

                    // skip abstract elements for now (however, we should treat them separately somehow)

                    continue;
                }
                iterateElement(elementDecl);
            }
        }
    }

    private static String getDeclarationName(final XSObject decl) {

        final String targetNameSpace = decl.getNamespace();

        return getDeclarationNameWithNamespace(decl, targetNameSpace);
    }

    private static String getDeclarationNameWithNamespace(final XSObject decl, final String targetNameSpace) {

        final String declName;

        if (targetNameSpace != null && !targetNameSpace.trim().isEmpty()) {

            if (targetNameSpace.endsWith(SLASH)) {

                throw new UnsupportedOperationException("Not yet implemented");
            } else {

                declName = targetNameSpace + HASH + decl.getName();
            }
        } else {

            declName = decl.getName();
        }

        return declName;
    }

    private static String getDeclarationName(final XSObject decl, final XSObject alternativeDecl) {

        final String declName = getDeclarationName(decl);

        if (declName.startsWith(HTTP_PREFIX)) {

            throw new UnsupportedOperationException("Not yet implemented");
        }

        return getDeclarationNameWithNamespace(decl, alternativeDecl.getNamespace());
    }

    private static boolean isMixed(final XSComplexTypeDefinition decl) {

        final short contentType = decl.getContentType();

        return XSComplexTypeDefinition.CONTENTTYPE_MIXED == contentType;
    }

    private static boolean isRepeated(final XSParticle particle) {

        return particle.getMaxOccursUnbounded() || particle.getMaxOccurs() > 1;
    }
}

