/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//jxpath/src/java/org/apache/commons/jxpath/PackageFunctions.java,v 1.1 2001/08/23 00:46:58 dmitri Exp $
 * $Revision: 1.1 $
 * $Date: 2001/08/23 00:46:58 $
 *
 * ====================================================================
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 2001, Plotnix, Inc,
 * <http://www.plotnix.com/>.
 * For more information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.commons.jxpath;

import java.util.*;
import java.lang.reflect.*;
import org.apache.commons.jxpath.functions.*;

/**
 * Extension functions provided by Java classes.  The class prefix specified
 * in the constructor is used when a constructor or a static method is called.
 * Usually, a class prefix is a package name (hence the name of this class).
 *
 * Let's say, we declared a PackageFunction like this:
 * <blockquote><pre>
 *     new PackageFunctions("java.util.", "util")
 * </pre></blockquote>
 *
 * We can now use XPaths like:
 * <dl>
 *  <dt><code>"util:Date.new()"</code></dt>
 *  <dd>Equivalent to <code>new java.util.Date()</code></dd>
 *  <dt><code>"util:Collections.singleton('foo')"</code></dt>
 *  <dd>Equivalent to <code>java.util.Collections.singleton("foo")</code></dd>
 *  <dt><code>"util:substring('foo', 1, 2)"</code></dt>
 *  <dd>Equivalent to <code>"foo".substring(1, 2)</code>.  Note that in
 *  this case, the class prefix is not used. JXPath does not check that
 *  the first parameter of the function (the method target) is in fact
 *  a member of the package described by this PackageFunctions object.</dd>
 * </dl>
 *
 * There is one PackageFunctions object registered by default with each
 * JXPathContext.  It does not have a namespace and uses no class prefix.
 * The existence of this object allows us to use XPaths like:
 * <code>"java.util.Date.new()"</code> and <code>"length('foo')"</code>
 * without the explicit registration of any extension functions.
 *
 * @author Dmitri Plotnikov
 * @version $Revision: 1.1 $ $Date: 2001/08/23 00:46:58 $
 */
public class PackageFunctions implements Functions {
    private String classPrefix;
    private String namespace;
    private static final Object[] EMPTY_ARRAY = new Object[0];

    public PackageFunctions(String classPrefix, String namespace){
        this.classPrefix = classPrefix;
        this.namespace = namespace;
    }

    public Set getUsedNamespaces(){
        return Collections.singleton(namespace);
    }

    /**
     * Returns a Function, if any, for the specified namespace,
     * name and parameter types.
     */
    public Function getFunction(String namespace, String name, Object[] parameters){
        if ((namespace == null && this.namespace != null) ||
                (namespace != null && !namespace.equals(this.namespace))){
            return null;
        }

        if (parameters == null){
            parameters = EMPTY_ARRAY;
        }

        if (parameters.length >= 1){
            Object target = parameters[0];
            if (target != null){
                if (target instanceof ExpressionContext){
                    target = ((ExpressionContext)target).getContextNodePointer().getValue();
                }
            }
            if (target != null){
                Method method = Types.lookupMethod(Object.class, name, parameters);
                if (method != null){
                    return new MethodFunction(method);
                }
            }
        }

        String fullName = classPrefix + name;
        int inx = fullName.lastIndexOf('.');
        if (inx == -1){
            return null;
        }

        String className = fullName.substring(0, inx);
        String methodName = fullName.substring(inx + 1);

        Class functionClass;
        try {
            functionClass = Class.forName(className);
        }
        catch (ClassNotFoundException ex){
            throw new RuntimeException("Class not found: " + ex);
        }

        if (methodName.endsWith("new")){
            Constructor constructor = Types.lookupConstructor(functionClass, parameters);
            if (constructor != null){
                return new ConstructorFunction(constructor);
            }
        }
        else {
            Method method = Types.lookupStaticMethod(functionClass, methodName, parameters);
            if (method != null){
                return new MethodFunction(method);
            }
        }
        return null;
    }
}