package com.vectrace.MercurialEclipse.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgParentClient {

    private static class Rev implements Comparable<Rev>{
        final int id;
        Rev p1;
        Rev p2;
        
        private Rev(int id) {
            this.id = id;
        }
        
        @Override
        public String toString() {
            return "r"+id;
        }
        
        @Override
        public int hashCode() {
            return id;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Rev other = (Rev) obj;
            if (id != other.id)
                return false;
            return true;
        }

        public int compareTo(Rev o) {
            return this.id-o.id;
        }
    }
    
    public static int[] getParents(IProject project) throws HgException {
        HgCommand command = new HgCommand("parents", project, false);
        command.addOptions("--template", "{rev}\n");
        String[] lines = command.executeToString().split("\n");
        int[] parents = new int[lines.length];
        for(int i=0; i<lines.length; i++) {
            parents[i] = Integer.parseInt(lines[i]);
        }
        return parents;
    }
    
    public static int findCommonAncestor(IProject project, int r1, int r2) throws HgException {
        int rMin = Math.min(r1, r2);
        int rMax = Math.max(r1, r2);
        //construct graph
        HgCommand command = new HgCommand("log", project, false);
        command.addOptions("--template", "{rev} {parents}\n", "-r", "0:"+rMax);
        String[] lines = command.executeToString().split("\n");
        Pattern pattern = Pattern.compile("^([0-9]+) (([0-9]+):[0-9a-f]+ (([0-9]+):[0-9a-f]+ )?)?$");
        Map<String, Rev> revs = new HashMap<String, Rev>(rMax);
        for(String line: lines) {
            Matcher m = pattern.matcher(line);
            if(m.matches()) {
                String id = m.group(1);
                Rev r = new Rev(Integer.parseInt(id));
                revs.put(id, r);
                if(m.group(3)!=null) {
                    r.p1 = revs.get(m.group(3));
                    if(m.group(5)!=null) {
                        r.p2 = revs.get(m.group(5));
                    }
                } else if(r.id != 0) {
                    r.p1 = revs.get(Integer.toString(r.id-1));
                }
            } else {
                throw new HgException("Unexpected format");
            }
        }
        //find 1st common ancestor
        //TODO find a more efficient algorithm
        TreeSet<Rev> ancestorsMin = getAncestors(revs.get(Integer.toString(rMin)));
        TreeSet<Rev> ancestorsMax = getAncestors(revs.get(Integer.toString(rMax)));
        ancestorsMin.retainAll(ancestorsMax);
        return ancestorsMin.last().id;
    }
    
    private static TreeSet<Rev> getAncestors(Rev r) {
        TreeSet<Rev> result = new TreeSet<Rev>();
        TreeSet<Rev> lastGeneration = new TreeSet<Rev>();
        TreeSet<Rev> currentGeneration;
        result.add(r);
        lastGeneration.add(r);
        while(!lastGeneration.isEmpty()) {
            currentGeneration = new TreeSet<Rev>();
            for(Rev rev: lastGeneration) {
                if(rev.p1 != null) {
                    currentGeneration.add(rev.p1);
                    result.add(rev.p1);
                    if(rev.p2 != null) {
                        currentGeneration.add(rev.p2);
                        result.add(rev.p2);
                    }
                }
            }
            lastGeneration = currentGeneration;
        }
        return result;
    }
}
