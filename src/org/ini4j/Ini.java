/**
 * Copyright 2005,2009 Ivan SZKIBA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ini4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;


import java.net.URL;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ini extends MultiMapImpl<String, Ini.Section>
{
    private static final char SUBST_CHAR = '$';
    private static final String SECTION_SYSTEM_PROPERTIES = "@prop";
    private static final String SECTION_ENVIRONMENT = "@env";
    private static final Pattern expr = Pattern.compile("(?<!\\\\)\\$\\{(([^\\[]+)(\\[([0-9]+)\\])?/)?([^\\[]+)(\\[(([0-9]+))\\])?\\}");
    private static final int G_SECTION = 2;
    private static final int G_SECTION_IDX = 4;
    private static final int G_OPTION = 5;
    private static final int G_OPTION_IDX = 7;
    private Config _config = Config.getGlobal();

    public Ini()
    {
    }

    public Ini(Reader input) throws IOException, InvalidIniFormatException
    {
        this();
        load(input);
    }

    public Ini(InputStream input) throws IOException, InvalidIniFormatException
    {
        this();
        load(input);
    }

    public Ini(URL input) throws IOException, InvalidIniFormatException
    {
        this();
        load(input);
    }

    public void setConfig(Config value)
    {
        _config = value;
    }

    public Section add(String name)
    {
        Section s = new Section(name);

        if (getConfig().isMultiSection())
        {
            add(name, s);
        }
        else
        {
            put(name, s);
        }

        return s;
    }

    public void load(InputStream input) throws IOException, InvalidIniFormatException
    {
        load(new InputStreamReader(input));
    }

    public void load(Reader input) throws IOException, InvalidIniFormatException
    {
        Builder builder = new Builder();

        IniParser.newInstance(getConfig()).parse(input, builder);
    }

    public void load(URL input) throws IOException, InvalidIniFormatException
    {
        Builder builder = new Builder();

        IniParser.newInstance(getConfig()).parse(input, builder);
    }

    public void loadFromXML(InputStream input) throws IOException, InvalidIniFormatException
    {
        loadFromXML(new InputStreamReader(input));
    }

    public void loadFromXML(Reader input) throws IOException, InvalidIniFormatException
    {
        Builder builder = new Builder();

        IniParser.newInstance(getConfig()).parseXML(input, builder);
    }

    public void loadFromXML(URL input) throws IOException, InvalidIniFormatException
    {
        Builder builder = new Builder();

        IniParser.newInstance(getConfig()).parseXML(input, builder);
    }

    public Section remove(Section section)
    {
        return remove(section.getName());
    }

    public void store(OutputStream output) throws IOException
    {
        store(IniFormatter.newInstance(output, getConfig()));
    }

    public void store(Writer output) throws IOException
    {
        store(IniFormatter.newInstance(output, getConfig()));
    }

    protected Config getConfig()
    {
        return _config;
    }

    protected void resolve(StringBuilder buffer, Section owner)
    {
        Matcher m = expr.matcher(buffer);

        while (m.find())
        {
            if (m.groupCount() < G_OPTION_IDX)
            {
                continue;
            }

            String sectionName = m.group(G_SECTION);
            String optionName = m.group(G_OPTION);
            int sectionIndex = (m.group(G_SECTION_IDX) == null) ? 0 : Integer.parseInt(m.group(G_SECTION_IDX));
            int optionIndex = (m.group(G_OPTION_IDX) == null) ? 0 : Integer.parseInt(m.group(G_OPTION_IDX));
            Section section = (sectionName == null) ? owner : get(sectionName, sectionIndex);
            String value;

            if (SECTION_ENVIRONMENT.equals(sectionName))
            {
                value = System.getenv(optionName);
            }
            else if (SECTION_SYSTEM_PROPERTIES.equals(sectionName))
            {
                value = System.getProperty(optionName);
            }
            else
            {
                value = (section == null) ? null : section.fetch(optionName, optionIndex);
            }

            if (value != null)
            {
                buffer.replace(m.start(), m.end(), value);
                m.reset(buffer);
            }
        }
    }

    protected void store(IniHandler formatter) throws IOException
    {
        formatter.startIni();
        for (Ini.Section s : values())
        {
            formatter.startSection(s.getName());
            for (String name : s.keySet())
            {
                int n = getConfig().isMultiOption() ? s.length(name) : 1;

                for (int i = 0; i < n; i++)
                {
                    formatter.handleOption(name, s.get(name, i));
                }
            }

            formatter.endSection();
        }

        formatter.endIni();
    }

    public class Section extends MultiMapImpl<String, String>
    {
        private String _name;

        public Section(String name)
        {
            super();
            _name = name;
        }

        public String getName()
        {
            return _name;
        }

        public String fetch(Object key)
        {
            return fetch(key, 0);
        }

        public String fetch(Object key, int index)
        {
            String value = get(key, index);

            if ((value != null) && (value.indexOf(SUBST_CHAR) >= 0))
            {
                StringBuilder buffer = new StringBuilder(value);

                resolve(buffer, this);
                value = buffer.toString();
            }

            return value;
        }

    }


    class Builder implements IniHandler
    {
        private Section currentSection;

        public void endIni()
        {
        }

        public void endSection()
        {
            currentSection = null;
        }

        public void handleOption(String name, String value)
        {
            if (getConfig().isMultiOption())
            {
                currentSection.add(name, value);
            }
            else
            {
                currentSection.put(name, value);
            }
        }

       
        public void startIni()
        {
        }

        public void startSection(String sectionName)
        {
            if (getConfig().isMultiSection())
            {
                currentSection = add(sectionName);
            }
            else
            {
                Section s = get(sectionName);

                currentSection = (s != null) ? s : add(sectionName);
            }
        }
    }
}
