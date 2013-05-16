package tw.idv.palatis.danboorugallery.model;

import java.util.Date;

public class Comment
{
	public int		id;
	public int		post_id;

	public int		creator_id;
	public String	creator;

	public int		score;

	public Date		created_at;
	public String	body;
}
