/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * zluspai	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.mylyn;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * Factory for mylyn facade. Used to avoid ClassCastException when mylyn is not available.
 *
 * @author zluspai
 *
 */
public class MylynFacadeFactory {

	/**
	 * Get the IMylynFacade instance.
	 * @return The mylyn facade
	 */
	public static IMylynFacade getMylynFacade() {
		Object facade = Proxy.newProxyInstance(MylynFacadeFactory.class.getClassLoader(), new Class[] {IMylynFacade.class}, new InvocationHandler() {

			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				try {
					MylynFacadeImpl impl = new MylynFacadeImpl();
					return method.invoke(impl, args);
				} catch (Throwable th) {
					MercurialEclipsePlugin.logError(th);
				}
				return null;
			}
		});
		return (IMylynFacade) facade;
	}

}
