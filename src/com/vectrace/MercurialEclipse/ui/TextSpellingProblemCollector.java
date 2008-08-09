/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.Iterator;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;

/**
 * @author bastian
 * 
 */
public class TextSpellingProblemCollector implements ISpellingProblemCollector {
    
    private ISourceViewer sourceViewer;

    /**
     * 
     */
    public TextSpellingProblemCollector(ISourceViewer viewer) {
        this.sourceViewer = viewer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept(org
     * .eclipse.ui.texteditor.spelling.SpellingProblem)
     */
    public void accept(SpellingProblem problem) {        
        Annotation ann = new SpellingAnnotation(problem);
        Position pos = new Position(problem.getOffset(), problem.getLength());
        sourceViewer.getAnnotationModel().addAnnotation(ann, pos);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#beginCollecting
     * ()
     */
    @SuppressWarnings("unchecked")
    public void beginCollecting() {
        IAnnotationModel am = sourceViewer.getAnnotationModel();
        Iterator<Annotation> iter = am.getAnnotationIterator();
        while (iter.hasNext()) {
            am.removeAnnotation(iter.next());
        }        
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#endCollecting
     * ()
     */
    public void endCollecting() {
    }

}
