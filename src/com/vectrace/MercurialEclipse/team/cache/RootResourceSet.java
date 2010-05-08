/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * lordofthepigs	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.model.HgRoot;

final class RootResourceSet {

	private final Map<HgRoot, Set<IResource>> resources = new HashMap<HgRoot, Set<IResource>>();

	RootResourceSet(){
		super();
	}

	public void add(HgRoot root, IResource resource){
		if(root == null){
			throw new IllegalArgumentException("HgRoot is null");
		}
		Set<IResource> set = resources.get(root);
		if(set == null){
			set = new HashSet<IResource>();
			resources.put(root, set);
		}
		set.add(resource);
	}

	public void addAll(RootResourceSet that){
		for(Map.Entry<HgRoot, Set<IResource>> entry : that.resources.entrySet()){
			if(resources.containsKey(entry.getKey())){
				resources.get(entry.getKey()).addAll(entry.getValue());
			}else{
				resources.put(entry.getKey(), entry.getValue());
			}
		}
	}

	public boolean contains(IResource resource){
		for(Set<IResource> set : this.resources.values()){
			if(set.contains(resource)){
				return true;
			}
		}

		return false;
	}

	public int size(){
		int size = 0;

		for(Set<IResource> set :resources.values()){
			size += set.size();
		}

		return size;
	}

	public boolean isEmpty(){
		return this.size() == 0;
	}

	public void clear(){
		this.resources.clear();
	}

	public HgRoot rootOf(IResource res){
		for(Map.Entry<HgRoot, Set<IResource>> entry : this.resources.entrySet()){
			if(entry.getValue().contains(res)){
				return entry.getKey();
			}
		}
		return null;
	}

	public Set<Map.Entry<HgRoot, Set<IResource>>> entrySet(){
		return this.resources.entrySet();
	}

	public boolean remove(IResource res){
		for(Set<IResource> set : this.resources.values()){
			if(set.remove(res)){
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode(){
		return 23 * this.resources.hashCode();
	}

	@Override
	public boolean equals(Object o){
		if(o == null || !o.getClass().equals(this.getClass())){
			return false;
		}
		RootResourceSet that = (RootResourceSet)o;
		return this.resources.equals(that.resources);
	}
}