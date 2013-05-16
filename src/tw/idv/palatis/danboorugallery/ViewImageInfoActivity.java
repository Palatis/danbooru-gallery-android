package tw.idv.palatis.danboorugallery;

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

import tw.idv.palatis.danboorugallery.model.Post;
import tw.idv.palatis.danboorugallery.utils.ImageLoader;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ViewImageInfoActivity
	extends Activity
{
	ImageLoader			loader	= ImageLoader.getInstance();
	Post				post;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.view_image_info );

		Intent intent = getIntent();
		post = intent.getParcelableExtra( "post" );

		TextView info = (TextView) findViewById( R.id.view_image_info_info );
		info.setText( getString( R.string.view_image_info_post_info, post.width, post.height, post.author, post.created_at == null ? getString( R.string.view_image_info_nulltime ) : post.created_at.toString() ) );
	}
}