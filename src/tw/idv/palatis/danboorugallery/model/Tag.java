package tw.idv.palatis.danboorugallery.model;

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
	implements Comparable < Tag >
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

	@Override
	public int compareTo(Tag another)
	{
		return name.compareTo( another.name );
	}
}
