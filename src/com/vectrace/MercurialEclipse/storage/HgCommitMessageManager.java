/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Zingo Andersen           - Save/Load commit messages using a xml file
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;



import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * A manager for all Mercurial commit messages.
 * The commit messages are save to a xml file when closing down and then re-read when the plugin is started.
 * 
 */
public class HgCommitMessageManager extends DefaultHandler {

    /*
     * commit messages database (keep it simple) 
     */
    private static String[] commit_message = new String[0];
    
    final static private String COMMIT_MESSAGE_FILE = "commit_messages.xml"; //$NON-NLS-1$
    final static private String XML_TAG_COMMIT_MESSAGE  = "commitmessage";   //$NON-NLS-1$
    final static private String XML_TAG_COMMIT_MESSAGES = "commitmessages";  //$NON-NLS-1$

    String tmpMessage;

    /**
     *  Save message in in-memory database
     */    
    
    public void saveCommitMessage(String message) {
        int new_size = commit_message.length + 1 ;

        int prefs_commit_message_size_max = Integer.parseInt(MercurialUtilities
                .getPreference(MercurialPreferenceConstants.COMMIT_MESSAGE_BATCH_SIZE,
                        "10")); //$NON-NLS-1$
        
        if(new_size == (prefs_commit_message_size_max + 1))
        {
            /* we have a full buffer just shift around in it no need for a new buffer */
            new_size = prefs_commit_message_size_max;

            /* shift everything down */
            for(int i = (new_size - 1); i > 0; i--)
            {
                commit_message[i] = commit_message[i - 1];
            }
            commit_message[0] = message; /* put new message first */
        }
        else
        {
            if(new_size > (prefs_commit_message_size_max + 1))
            {   
                /* This probably means that the prefs size got smaller then the old buffer 
                 * lets copy it to a smaller buffer 
                 */
                new_size = prefs_commit_message_size_max;
            }
            String commit_message2[] = new String[new_size];
            commit_message2[0] = message; /* put new message first */
            for(int i = 1; i < new_size; i++)
            {
                commit_message2[i] = commit_message[i - 1];
            }

            /* Replace the comment string array */
            commit_message = commit_message2;
        }
    }

    /**
     *  Save message in in-memory database new data last (used when loading from file)
     */    
    
    private void addCommitMessage(String message) {
        int new_size = commit_message.length + 1;

        int prefs_commit_message_size_max = Integer.parseInt(MercurialUtilities
                .getPreference(MercurialPreferenceConstants.COMMIT_MESSAGE_BATCH_SIZE,
                        "10")); //$NON-NLS-1$

        /* only add new stuff if its lower or equal the prefs size */
        if(new_size <= prefs_commit_message_size_max)
        {
            String commit_message2[] = new String[new_size];
            for(int i = 0; i < (new_size - 1); i++)
            {
                commit_message2[i] = commit_message[i];
            }
            commit_message2[new_size - 1] = message; /* put new message last */

            /* Replace the comment string array */
            commit_message = commit_message2;
        }
    }

    
    /**
     *  Get all messages from in-memory database
     */        
    public String[] getCommitMessages() {
        int size = commit_message.length;
        int prefs_commit_message_size_max = Integer.parseInt(MercurialUtilities
                .getPreference(MercurialPreferenceConstants.COMMIT_MESSAGE_BATCH_SIZE,
                        "10")); //$NON-NLS-1$
        if(size > (prefs_commit_message_size_max ))
        {
            /*
             *  prefs changed to smaller since last used copy to smaller buffer
             */
            String commit_message2[] = new String[prefs_commit_message_size_max];
            for(int i = 0; i < prefs_commit_message_size_max; i++)
            {
                commit_message2[i] = commit_message[i];
            }

            /* Replace the comment string array */
            commit_message = commit_message2;
        }
        return commit_message;
    }
    
    /**
     * Return a <code>File</code> object representing the location file. The
     * file may or may not exist and must be checked before use.
     */
    private File getLocationFile() {
        return MercurialEclipsePlugin.getDefault().getStateLocation().append(
                COMMIT_MESSAGE_FILE).toFile();
    }

    /**
     * Load all saved commit messages from the plug-in's default area.
     * 
     * @throws HgException
     */
    public void start() throws IOException, HgException {
        File file = getLocationFile();

        if (file.exists()) {
            /* String line; */
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "UTF-8")); //$NON-NLS-1$
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setValidating(false);
/*
            spf.setValidating(true);
*/
            try {
                SAXParser parser = parserFactory.newSAXParser();
                parser.parse(new InputSource(reader), this);
            }
            catch(SAXException e) {
                /* we don't want to load it - it will be cleaned when saving */
                MercurialEclipsePlugin.logError(e);
            } 
            catch(ParserConfigurationException e) {
                /* we don't want to load it - it will be cleaned when saving */
                MercurialEclipsePlugin.logError(e);
            }
            reader.close();
        }
    }

    /**
     * Save all commit messages from the in-memory database to the plug-in's default area.
     */
    public void stop() throws IOException {
        File file = getLocationFile();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$
        
        StreamResult streamResult = new StreamResult(writer);
        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) TransformerFactory.newInstance();

        try {
            TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
            Transformer transformer = transformerHandler.getTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1"); //$NON-NLS-1$
/*
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"mercurialeclipse_commitmessage.dtd"); //$NON-NLS-1$
*/
            transformer.setOutputProperty(OutputKeys.INDENT,"yes"); //$NON-NLS-1$
            transformerHandler.setResult(streamResult);
            transformerHandler.startDocument();
            
            AttributesImpl atts = new AttributesImpl();
            atts.clear();
            transformerHandler.startElement("","",XML_TAG_COMMIT_MESSAGES,atts); //$NON-NLS-1$

            int size = commit_message.length;

            int prefs_commit_message_size_max = Integer.parseInt(MercurialUtilities
                    .getPreference(MercurialPreferenceConstants.COMMIT_MESSAGE_BATCH_SIZE,
                            "10")); //$NON-NLS-1$

            /* Do not save more then the prefs size */
            if(size > prefs_commit_message_size_max)
            {
                size = prefs_commit_message_size_max;
            }

            
            for (int i = 0; i < size; i++)
            {
                transformerHandler.startElement("","",XML_TAG_COMMIT_MESSAGE,atts); //$NON-NLS-1$
                transformerHandler.characters(commit_message[i].toCharArray(), 0, commit_message[i].length()); //$NON-NLS-1$
                transformerHandler.endElement("","",XML_TAG_COMMIT_MESSAGE); //$NON-NLS-1$
            }
            transformerHandler.endElement("","",XML_TAG_COMMIT_MESSAGES); //$NON-NLS-1$
            transformerHandler.endDocument();
        } catch (TransformerConfigurationException e) {
            MercurialEclipsePlugin.logError(e);
        } catch (IllegalArgumentException e) {
            MercurialEclipsePlugin.logError(e);
        } catch (SAXException e) {
            MercurialEclipsePlugin.logError(e);
        }
        
        writer.close();
    }
    
    
    /*
     *  SAX Handler methods class to handle XML parsing
     */

    /**
     * Called when the starting of the Element is reached. For Example if we have Tag
     * called <Title> ... </Title>, then this method is called when <Title> tag is
     * Encountered while parsing the Current XML File. The AttributeList Parameter has
     * the list of all Attributes declared for the Current Element in the XML File.
     */
    @Override
    public void startElement(String uri, String localName, String qname, 
            Attributes attr)
    {
        /* Clear char string */
        tmpMessage = ""; //$NON-NLS-1$
    }

    /**
     * Called when the Ending of the current Element is reached. For example in the
     * above explanation, this method is called when </Title> tag is reached
     */
    @Override
    public void endElement(String uri, String localName, String qname) {
        /* If it was a commit message save the char string in the database */
        if (qname.equalsIgnoreCase(XML_TAG_COMMIT_MESSAGE)) { 
            addCommitMessage(tmpMessage);
        }            
    }

    /**
     * While Parsing the XML file, if extra characters like space or enter Character
     * are encountered then this method is called. If you don't want to do anything
     * special with these characters, then you can normally leave this method blank.
     */
    @Override
    public void characters(char[] ch, int start, int length) {
        /* Collect the char string together this will be called for every special char */
        tmpMessage = tmpMessage + new String(ch, start, length);
    }
}
