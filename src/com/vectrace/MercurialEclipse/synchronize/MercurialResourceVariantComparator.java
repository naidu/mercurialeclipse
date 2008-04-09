package com.vectrace.MercurialEclipse.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

public class MercurialResourceVariantComparator implements
		IResourceVariantComparator {
	private static MercurialResourceVariantComparator instance;
	
	private MercurialResourceVariantComparator() {		
	}
	
	public static MercurialResourceVariantComparator getInstance(){
		if (instance == null){
			instance = new MercurialResourceVariantComparator();
		}
		return instance;
	}

	public boolean compare(IResource local, IResourceVariant remote) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean compare(IResourceVariant base, IResourceVariant remote) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isThreeWay() {
		// TODO Auto-generated method stub
		return false;
	}

}
