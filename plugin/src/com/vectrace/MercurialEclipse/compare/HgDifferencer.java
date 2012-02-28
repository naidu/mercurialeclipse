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
package com.vectrace.MercurialEclipse.compare;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;

/**
 * Compare sparse trees for performance
 */
public class HgDifferencer extends Differencer {

	@Override
	protected Object visit(Object data, int result, Object ancestor, Object left, Object right) {
		DiffNode node = null;

		Object[] ancestorChildren = getChildren(ancestor);
		Object[] rightChildren = getChildren(right);
		Object[] leftChildren = getChildren(left);

		if ((left == null && rightChildren != null && rightChildren.length > 0)
				|| (right == null && leftChildren != null && leftChildren.length > 0)) {

			node = new DiffNode((IDiffContainer) data, Differencer.CHANGE,
					(ITypedElement) ancestor, (ITypedElement) left, (ITypedElement) right);

			Set<Object> allSet = new HashSet<Object>(20);
			Map<Object, Object> ancestorSet = null;
			Map<Object, Object> rightSet = null;
			Map<Object, Object> leftSet = null;

			if (ancestorChildren != null) {
				ancestorSet = new HashMap<Object, Object>(10);
				for (int i = 0; i < ancestorChildren.length; i++) {
					Object ancestorChild = ancestorChildren[i];
					ancestorSet.put(ancestorChild, ancestorChild);
					allSet.add(ancestorChild);
				}
			}

			if (rightChildren != null) {
				rightSet = new HashMap<Object, Object>(10);
				for (int i = 0; i < rightChildren.length; i++) {
					Object rightChild = rightChildren[i];
					rightSet.put(rightChild, rightChild);
					allSet.add(rightChild);
				}
			}

			if (leftChildren != null) {
				leftSet = new HashMap<Object, Object>(10);
				for (int i = 0; i < leftChildren.length; i++) {
					Object leftChild = leftChildren[i];
					leftSet.put(leftChild, leftChild);
					allSet.add(leftChild);
				}
			}

			Iterator<Object> e = allSet.iterator();
			while (e.hasNext()) {
				Object keyChild = e.next();
				Object ancestorChild = ancestorSet != null ? ancestorSet.get(keyChild) : null;
				Object leftChild = leftSet != null ? leftSet.get(keyChild) : null;
				Object rightChild = rightSet != null ? rightSet.get(keyChild) : null;
				visit(node, result, ancestorChild, leftChild, rightChild);
			}

		} else {
			node = new DiffNode((IDiffContainer) data, result, (ITypedElement) ancestor,
					(ITypedElement) left, (ITypedElement) right);
		}

		return node;
	}

}
