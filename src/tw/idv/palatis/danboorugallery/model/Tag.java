package tw.idv.palatis.danboorugallery.model;

import java.util.Comparator;

import org.json.JSONObject;

/**
 * this is the data structure used to represents a tag.
 * for example (json):
 * {"type":0,"count":2723,"ambiguous":false,"name":"^_^","id":402217}
 * or (xml):
 * <tag type="4" count="20" ambiguous="false" name="scarmiglione" id="397732"/>
 *
 * @author palatis
 */
public class Tag
{
	public int		id;
	public int		type;
	public int		count;
	public boolean	ambiguous;
	public String	name;

	public Tag()
	{
	}

	public Tag(JSONObject json)
	{
		id = json.optInt( "id" );
		type = json.optInt( "type" );
		count = json.optInt( "count" );
		ambiguous = json.optBoolean( "ambiguous" );
		name = json.optString( "name" );
	}

	public static class CompareByCount implements Comparator<Tag>
	{
		@Override
		public int compare(Tag object1, Tag object2)
		{
			return object1.count - object2.count;
		}
	}

	public static class CompareByName implements Comparator<Tag>
	{
		@Override
		public int compare(Tag object1, Tag object2)
		{
			return object1.name.compareTo( object2.name );
		}
	}

	public static class CompareById implements Comparator<Tag>
	{
		@Override
		public int compare(Tag object1, Tag object2)
		{
			return object1.id - object2.id;
		}
	}
}
