/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * ge.zhong	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

/**
 * @author Ge Zhong
 *
 */
public interface IHgFolder extends IHgResource {

	public IHgResource[] members();
}
