package tw.idv.palatis.danboorugallery.model;

/*
 * This file is part of Danbooru Gallery
 *
 * Copyright 2011
 *   - Victor Tseng <palatis@gmail.com>
 *
 * Danbooru Gallery is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Danbooru Gallery is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Danbooru Gallery.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class Hosts
{
	static public final int	HOST_NAME	= 0;
	static public final int	HOST_URL	= 1;

	List < String[] >		hosts;

	public Hosts()
	{
		this( new ArrayList < String[] >() );
	}

	private Hosts(List < String[] > h)
	{
		hosts = h;
	}

	public boolean add(String name, String url)
	{
		return hosts.add( new String[] {
			name,
			url
		} );
	}

	public boolean add(String[] host) throws IllegalArgumentException
	{
		if (host.length != 2)
			throw new IllegalArgumentException( "\"host\" must be a String[2] with index HOSTS_NAME as name and index HOSTS_URL as url." );

		return hosts.add( host );
	}

	public void set(int location, String name, String url)
	{
		String host[] = new String[] {
			name,
			url
		};
		hosts.set( location, host );
	}

	public String[] get(int location)
	{
		if (hosts.isEmpty())
			return null;

		if (location < hosts.size())
			return hosts.get( location );

		return hosts.get( hosts.size() - 1 );
	}

	public String[] remove(int location) throws IndexOutOfBoundsException
	{
		return hosts.remove( location );
	}

	public String toCSV()
	{
		StringWriter strwriter = new StringWriter();
		CSVWriter csvwriter = new CSVWriter( strwriter );
		csvwriter.writeAll( hosts );
		return strwriter.toString();
	}

	public int size()
	{
		return hosts.size();
	}

	// Factory Method
	static public Hosts fromCSV(String csv_str) throws IOException
	{
		CSVReader reader = new CSVReader( new StringReader( csv_str ) );
		return new Hosts( reader.readAll() );
	}
}
