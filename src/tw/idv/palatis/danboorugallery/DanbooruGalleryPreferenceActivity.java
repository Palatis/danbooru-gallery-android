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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import tw.idv.palatis.danboorugallery.defines.D;
import tw.idv.palatis.danboorugallery.model.Host;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class DanbooruGalleryPreferenceActivity
	extends PreferenceActivity
{
	SharedPreferences			preferences;
	List < Host >				hosts;

	HostDialogOnClickListener	host_dialog_listener;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );
		addPreferencesFromResource( R.xml.danbooru_gallery_preference );

		preferences = getSharedPreferences( D.SHAREDPREFERENCES_NAME, MODE_PRIVATE );
		host_dialog_listener = new HostDialogOnClickListener();
	}

	@Override
	protected void onStart()
	{
		if (preferences.contains( "json_hosts" ))
			try
			{
				hosts = D.HostsFromJSONArray( new JSONArray( preferences.getString( "json_hosts", "" ) ) );
			}
			catch (JSONException ex)
			{
				D.Log.wtf( ex );
			}
		else
			hosts = new ArrayList < Host >();

		try
		{
			PackageInfo manager = getPackageManager().getPackageInfo( getPackageName(), 0 );
			findPreference( "preferences_about_version" ).setSummary( manager.versionName );
		}
		catch (NameNotFoundException e)
		{
			// Handle exception
		}

		ListPreference pref_hosts = (ListPreference) findPreference( "preferences_hosts_select" );
		pref_hosts.setOnPreferenceChangeListener( new OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				String name = hosts.get( Integer.parseInt( (String) newValue ) ).name;
				preference.setSummary( String.format( getString( R.string.preferences_hosts_selected_host ), name ) );
				return true;
			}
		} );

		CheckBoxPreference pref_aggressive = (CheckBoxPreference) findPreference( "preferences_options_aggressive_prefetch" );
		pref_aggressive.setChecked( preferences.getBoolean( "aggressive_prefetch", false ) );

		CheckBoxPreference pref_hqimage = (CheckBoxPreference) findPreference("preferences_options_high_quality_image" );
		pref_hqimage.setChecked( preferences.getBoolean( "high_quality_image", false ) );

		notifyHostsChanged();

		super.onStart();
	}

	private void notifyHostsChanged()
	{
		ListPreference pref_hosts = (ListPreference) findPreference( "preferences_hosts_select" );

		int selected_host_index = preferences.getInt( "selected_host", -1 );
		if (hosts.get( selected_host_index ) == null)
			selected_host_index = -1;

		pref_hosts.setSummary( String.format( getString( R.string.preferences_hosts_selected_host ), (selected_host_index == -1) ? "(NONE)" : hosts.get( selected_host_index ).name ) );

		android.preference.PreferenceCategory category = (PreferenceCategory) findPreference( "preferences_hosts_manage_category_manage" );
		category.removeAll();

		CharSequence pref_hosts_entries[] = new CharSequence[hosts.size()];
		CharSequence pref_hosts_entry_values[] = new CharSequence[hosts.size()];

		for (int i = 0; i < hosts.size(); ++i)
		{
			Host host = hosts.get( i );
			pref_hosts_entries[i] = host.name;
			pref_hosts_entry_values[i] = String.valueOf( i );

			Preference p = new Preference( this );
			p.setOrder( i );
			p.setTitle( host.name );
			p.setSummary( String.format( "%1$s\n(%2$s)", host.url, host.api ) );
			p.setKey( "auto_generated_host_entry" );
			category.addPreference( p );
		}

		pref_hosts.setEntries( pref_hosts_entries );
		pref_hosts.setEntryValues( pref_hosts_entry_values );
	}

	@Override
	public void onPause()
	{
		String selected_host = findPreference( "preferences_hosts_select" ).getSharedPreferences().getString( "preferences_hosts_select", "0" );
		String page_limit = findPreference( "preferences_options_page_limit" ).getSharedPreferences().getString( "preferences_options_page_limit", "16" );
		String rating = findPreference( "preferences_options_rating" ).getSharedPreferences().getString( "preferences_options_rating", "s" );
		boolean aggressive_prefetch = findPreference( "preferences_options_aggressive_prefetch" ).getSharedPreferences().getBoolean( "preferences_options_aggressive_prefetch", false );
		boolean high_quality_image = findPreference("preferences_options_high_quality_image" ).getSharedPreferences().getBoolean( "preferences_options_high_quality_image", false );

		SharedPreferences.Editor prefeditor = preferences.edit();
		prefeditor.putInt( "selected_host", Integer.parseInt( selected_host ) );
		prefeditor.putInt( "page_limit", Integer.parseInt( page_limit ) );
		prefeditor.putString( "rating", rating );
		prefeditor.putString( "json_hosts", D.JSONArrayFromHosts( hosts ).toString() );
		prefeditor.putBoolean( "aggressive_prefetch", aggressive_prefetch );
		prefeditor.putBoolean( "high_quality_image", high_quality_image );
		prefeditor.apply();

		super.onPause();
	}

	private BaseAdapter	mSiteApiAdapter	= new SiteApiAdapter();

	// FIXME: auto-detect available APIs
	private String[]	apis			= new String[] {
											"Danbooru - JSON",
											"Danbooru - XML",
											"Gelbooru - XML",
											"Shimmie2 - XML",
											"Shimmie2 - RSS",
										};

	private class SiteApiAdapter
		extends BaseAdapter
	{
		@Override
		public int getCount()
		{
			return apis.length;
		}

		@Override
		public Object getItem(int position)
		{
			return apis[position];
		}

		@Override
		public long getItemId(int position)
		{
			return apis[position].hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			TextView text = null;
			if (convertView != null)
				text = (TextView) convertView;
			if (text == null)
			{
				text = new TextView( DanbooruGalleryPreferenceActivity.this );
				text.setTextColor( 0xff000000 );
				text.setPadding( 8, 8, 8, 8 );
			}

			text.setText( apis[position] );

			return text;
		}
	};

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
	{
		if (preference.getKey() != null)
		{
			if (preference.getKey().equals( "preferences_about_license" ))
			{
				Builder builder = new AlertDialog.Builder( this );
				builder.setTitle( R.string.preferences_about_license_dialog_title );
				builder.setMessage( R.string.preferences_about_license_dialog_content );
				builder.setPositiveButton( android.R.string.ok, null );
				builder.create().show();
				return true;
			}
			else if (preference.getKey().equals( "preferences_hosts_manage_new" ))
			{
				View dialog_view = getLayoutInflater().inflate( R.layout.host_dialog, null );
				Spinner api = (Spinner) dialog_view.findViewById( R.id.preferences_hosts_dialog_api );
				api.setPromptId( R.string.preferences_hosts_dialog_api );
				api.setAdapter( mSiteApiAdapter );
				api.setSelection( 0 );

				Builder builder = new AlertDialog.Builder( this );
				builder.setTitle( R.string.preferences_hosts_dialog_title );
				builder.setView( dialog_view );
				builder.setPositiveButton( android.R.string.ok, host_dialog_listener );
				builder.setNegativeButton( android.R.string.cancel, null );
				builder.create().show();
				return true;
			}
			else if (preference.getKey().equals( "auto_generated_host_entry" ))
			{
				Host host = hosts.get( preference.getOrder() );

				View dialog_view = getLayoutInflater().inflate( R.layout.host_dialog, null );
				EditText edit_name = (EditText) dialog_view.findViewById( R.id.preferences_hosts_dialog_hosts_name_input );
				EditText edit_url = (EditText) dialog_view.findViewById( R.id.preferences_hosts_dialog_url_input );
				Spinner api = (Spinner) dialog_view.findViewById( R.id.preferences_hosts_dialog_api );
				edit_name.setTag( preference.getOrder() );
				edit_name.setText( host.name );
				edit_url.setText( host.url );
				api.setPromptId( R.string.preferences_hosts_dialog_api );
				api.setAdapter( mSiteApiAdapter );
				// FIXME: think yourself = =
				for (int i = 0 ;i < apis.length; ++i)
					if (host.api.equals( apis[i] ))
					{
						api.setSelection( i );
						break;
					}

				Builder builder = new AlertDialog.Builder( this );
				builder.setTitle( R.string.preferences_hosts_dialog_title );
				builder.setView( dialog_view );
				builder.setPositiveButton( android.R.string.ok, host_dialog_listener );
				builder.setNeutralButton( R.string.preferences_hosts_dialog_button_delete, host_dialog_listener );
				builder.setNegativeButton( android.R.string.cancel, null );

				builder.create().show();
			}
			D.Log.v( "preference clicked: %s", preference.getKey() );
		}
		return super.onPreferenceTreeClick( preferenceScreen, preference );
	}

	public void newHost(String name, String url, String api)
	{
		hosts.add( new Host( name, url, api ) );
		preferences.edit().putString( "json_hosts", D.JSONArrayFromHosts( hosts ).toString() ).apply();

		notifyHostsChanged();
	}

	public void editHost(int position, String name, String url, String api)
	{
		hosts.set( position, new Host( name, url, api ) );
		preferences.edit().putString( "json_hosts", D.JSONArrayFromHosts( hosts ).toString() ).apply();

		notifyHostsChanged();
	}

	public void deleteHost(int position)
	{
		// try to see if selected_host is after that host
		int selected_host = preferences.getInt( "selected_host", 0 );
		if (selected_host >= position)
			preferences.edit().putInt( "selected_host", selected_host - 1 ).apply();

		hosts.remove( position );
		preferences.edit().putString( "json_hosts", D.JSONArrayFromHosts( hosts ).toString() ).apply();

		notifyHostsChanged();
	}

	private class HostDialogOnClickListener
		implements OnClickListener
	{
		@Override
		public void onClick(DialogInterface idialog, int which)
		{
			final AlertDialog dialog = (AlertDialog) idialog;
			final EditText edit_name = (EditText) dialog.findViewById( R.id.preferences_hosts_dialog_hosts_name_input );
			final EditText edit_url = (EditText) dialog.findViewById( R.id.preferences_hosts_dialog_url_input );
			final Spinner api = (Spinner) dialog.findViewById( R.id.preferences_hosts_dialog_api );
			switch (which)
			{
			case DialogInterface.BUTTON_POSITIVE: // OK
				if (edit_name.getTag() == null)
					newHost( edit_name.getText().toString(), edit_url.getText().toString(), api.getSelectedItem().toString() );
				else
					editHost( (Integer) edit_name.getTag(), edit_name.getText().toString(), edit_url.getText().toString(), api.getSelectedItem().toString() );
				break;
			case DialogInterface.BUTTON_NEUTRAL: // Delete
				Builder builder = new AlertDialog.Builder( dialog.getContext() );
				builder.setTitle( R.string.preferences_hosts_dialog_delete_confirm_title );
				Host host = hosts.get( (Integer) edit_name.getTag() );
				builder.setMessage( String.format( getString( R.string.preferences_hosts_dialog_delete_confirm_message ), host.name, host.url ) );
				builder.setPositiveButton( android.R.string.ok, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						deleteHost( (Integer) edit_name.getTag() );
					}
				} );
				builder.setNegativeButton( android.R.string.cancel, null );
				builder.create().show();
				break;
			}
		}
	}
}