////////////////////////////////////////////////////////////////////////////////
// Danbooru Gallery Android - an danbooru-style imageboard browser
//     Copyright (C) 2014  Victor Tseng
//
//     This program is free software: you can redistribute it and/or modify
//     it under the terms of the GNU General Public License as published by
//     the Free Software Foundation, either version 3 of the License, or
//     (at your option) any later version.
//
//     This program is distributed in the hope that it will be useful,
//     but WITHOUT ANY WARRANTY; without even the implied warranty of
//     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//     GNU General Public License for more details.
//
//     You should have received a copy of the GNU General Public License
//     along with this program. If not, see <http://www.gnu.org/licenses/>
////////////////////////////////////////////////////////////////////////////////

package tw.idv.palatis.danboorugallery;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import tw.idv.palatis.danboorugallery.model.Host;
public class NewHostActivity
    extends Activity
{
    private static final String TAG = "NewHostActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_host);

        // if there is no action bar (ie. dialog), we need to display those buttons.
        if (getActionBar() != null)
        {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
            findViewById(R.id.dialog_new_host_panel_buttons).setVisibility(View.GONE);
        }
        else
        {
            findViewById(R.id.dialog_new_host_panel_buttons).setVisibility(View.VISIBLE);
            Button button;
            button = (Button)findViewById(R.id.dialog_new_host_button_save);
            button.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    NewHostFragment fragment = (NewHostFragment) getFragmentManager().findFragmentById(R.id.new_host_inputs_container);
                    fragment.saveHostToDatabase();
                }
            });
            button = (Button)findViewById(R.id.dialog_new_host_button_cancel);
            button.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Intent intent = new Intent(NewHostActivity.this, PostListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    navigateUpTo(intent);
                }
            });
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null)
        {
            Bundle arguments = savedInstanceState;
            if (arguments == null)
            {
                arguments = new Bundle();
                arguments.putInt(
                    Host.TABLE_NAME + Host.KEY_HOST_DATABASE_ID,
                    getIntent().getIntExtra(Host.TABLE_NAME + Host.KEY_HOST_DATABASE_ID, -1));
            }
            NewHostFragment fragment = new NewHostFragment();
            fragment.setArguments(arguments);
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.new_host_inputs_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.new_host_inputs_container);
        fragment.onCreateOptionsMenu(menu, getMenuInflater());
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            Intent intent = new Intent(this, PostListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            navigateUpTo(intent);
            return true;
        }

        Fragment fragment = getFragmentManager().findFragmentById(R.id.new_host_inputs_container);
        if (fragment.onOptionsItemSelected(item))
            return true;
        Log.d(TAG, item.getTitle().toString() + " clicked");
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged");
    }
}
