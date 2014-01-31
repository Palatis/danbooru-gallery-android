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
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import tw.idv.palatis.danboorugallery.database.HostsTable;
import tw.idv.palatis.danboorugallery.model.Host;
import tw.idv.palatis.danboorugallery.siteapi.SiteAPI;
import tw.idv.palatis.danboorugallery.util.SiteSession;

public class NewHostFragment
    extends Fragment
{
    private static final String TAG = "NewHostFragment";

    private int mHostId = -1;
    private EditText mEditTextName;
    private EditText mEditTextUrl;
    private EditText mEditTextLogin;
    private EditText mEditTextPassword;
    private Spinner mSpinnerSiteAPI;
    private Spinner mSpinnerPageLimitsStrict;
    private Spinner mSpinnerPageLimitsRelaxed;

    private static class ViewHolder
    {
        public TextView text1;

        public ViewHolder(View view)
        {
            text1 = (TextView) view.findViewById(android.R.id.text1);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_new_host, container, false);

        mEditTextName = (EditText)rootView.findViewById(R.id.dialog_new_host_input_name);
        mEditTextUrl = (EditText)rootView.findViewById(R.id.dialog_new_host_input_url);
        mEditTextLogin = (EditText)rootView.findViewById(R.id.dialog_new_host_input_login);
        mEditTextPassword = (EditText)rootView.findViewById(R.id.dialog_new_host_input_password);
        mSpinnerSiteAPI = (Spinner)rootView.findViewById(R.id.dialog_new_host_spinner_siteapi);
        mSpinnerPageLimitsStrict = (Spinner)rootView.findViewById(R.id.dialog_new_host_spinner_page_limits_strict);
        mSpinnerPageLimitsRelaxed = (Spinner)rootView.findViewById(R.id.dialog_new_host_spinner_page_limits_relaxed);

        SiteAPI.SiteAPIListAdapter adapter = new SiteAPI.SiteAPIListAdapter()
        {
            @Override
            public View getView(int position, View view, ViewGroup parent)
            {
                if (view == null)
                {
                    view = inflater.inflate(android.R.layout.simple_list_item_activated_1, parent, false);
                    view.setTag(R.id.view_tag_view_holder, new ViewHolder(view));
                }

                ViewHolder holder = (ViewHolder)view.getTag(R.id.view_tag_view_holder);
                holder.text1.setText(getResources().getString(R.string.dialog_new_host_siteapi, getItem(position).getName()));
                return view;
            }
        };
        mSpinnerSiteAPI.setAdapter(adapter);
        mSpinnerSiteAPI.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
            {
                SiteAPI api = (SiteAPI)adapterView.getAdapter().getItem(position);
                int value;

                value = Integer.parseInt(mSpinnerPageLimitsRelaxed.getSelectedItem().toString());
                SiteAPI.PageLimitAdapter pageLimitAdapterRelaxed = new PageLimitsAdapter(inflater, api, SiteAPI.PAGE_LIMIT_TYPE_RELAXED);
                mSpinnerPageLimitsRelaxed.setAdapter(pageLimitAdapterRelaxed);
                mSpinnerPageLimitsRelaxed.setSelection(pageLimitAdapterRelaxed.indexOf(value));

                value = Integer.parseInt(mSpinnerPageLimitsStrict.getSelectedItem().toString());
                SiteAPI.PageLimitAdapter pageLimitAdapterStrict = new PageLimitsAdapter(inflater, api, SiteAPI.PAGE_LIMIT_TYPE_STRICT);
                mSpinnerPageLimitsStrict.setAdapter(pageLimitAdapterStrict);
                mSpinnerPageLimitsStrict.setSelection(pageLimitAdapterStrict.indexOf(value));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        Bundle arguments = savedInstanceState;
        if (arguments == null)
            arguments = getArguments();
        if (arguments != null)
        {
            Host host = SiteSession.getHostById(arguments.getInt(Host.TABLE_NAME + Host.KEY_HOST_DATABASE_ID));
            if (host != null)
            {
                Log.d(TAG, "onCreateView(): " + host.toString());
                mHostId = host.id;
                mEditTextName.setText(host.name);
                mEditTextUrl.setText(host.url);
                mEditTextLogin.setText(host.getLogin());
                mEditTextPassword.setText(host.getPassword());
                mSpinnerSiteAPI.setSelection(adapter.indexOf(host.getAPI()));

                SiteAPI.PageLimitAdapter pageLimitAdapterStrict = new PageLimitsAdapter(inflater, host.getAPI(), SiteAPI.PAGE_LIMIT_TYPE_STRICT);
                mSpinnerPageLimitsStrict.setAdapter(pageLimitAdapterStrict);
                mSpinnerPageLimitsStrict.setSelection(pageLimitAdapterStrict.indexOf(host.pageLimitStrict));

                SiteAPI.PageLimitAdapter pageLimitAdapterRelaxed = new PageLimitsAdapter(inflater, host.getAPI(), SiteAPI.PAGE_LIMIT_TYPE_RELAXED);
                mSpinnerPageLimitsRelaxed.setAdapter(pageLimitAdapterRelaxed);
                mSpinnerPageLimitsRelaxed.setSelection(pageLimitAdapterRelaxed.indexOf(host.pageLimitRelaxed));
            }
            else
            {
                SiteAPI dummyapi = SiteAPI.getDummyAPI();

                SiteAPI.PageLimitAdapter pageLimitAdapterRelaxed = new PageLimitsAdapter(inflater, dummyapi, SiteAPI.PAGE_LIMIT_TYPE_RELAXED);
                mSpinnerPageLimitsRelaxed.setAdapter(pageLimitAdapterRelaxed);

                SiteAPI.PageLimitAdapter pageLimitAdapterStrict = new PageLimitsAdapter(inflater, dummyapi, SiteAPI.PAGE_LIMIT_TYPE_STRICT);
                mSpinnerPageLimitsStrict.setAdapter(pageLimitAdapterStrict);
            }
        }

        Activity activity = getActivity();
        if (activity != null)
            activity.setTitle(mHostId == -1 ? R.string.title_new_host : R.string.title_edit_host);

        return rootView;
    }

    private static class PageLimitsAdapter
            extends SiteAPI.PageLimitAdapter
    {
        private LayoutInflater mInflater;

        public PageLimitsAdapter(LayoutInflater inflater, SiteAPI api, int type)
        {
            super(api, type);
            mInflater = inflater;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent)
        {
            if (view == null)
            {
                view = mInflater.inflate(android.R.layout.simple_list_item_activated_1, parent, false);
                ViewHolder holder = new ViewHolder(view);
                view.setTag(R.id.view_tag_view_holder, holder);
                holder.text1.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            }

            ViewHolder holder = (ViewHolder)view.getTag(R.id.view_tag_view_holder);
            holder.text1.setText(getItem(position).toString());
            return view;
        }
    }
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt(Host.KEY_HOST_DATABASE_ID, mHostId);
        outState.putString(Host.KEY_HOST_NAME, mEditTextName.getText().toString());
        outState.putString(Host.KEY_HOST_URL, mEditTextUrl.getText().toString());
        outState.putString(Host.KEY_HOST_LOGIN, mEditTextLogin.getText().toString());
        outState.putString(Host.KEY_HOST_PASSWORD, mEditTextPassword.getText().toString());
        outState.putString(Host.KEY_HOST_API, ((SiteAPI)(mSpinnerSiteAPI.getSelectedItem())).getName());
        outState.putInt(Host.KEY_HOST_PAGE_LIMIT_STRICT, Integer.parseInt(mSpinnerPageLimitsStrict.getSelectedItem().toString()));
        outState.putInt(Host.KEY_HOST_PAGE_LIMIT_RELAXED, Integer.parseInt(mSpinnerPageLimitsRelaxed.getSelectedItem().toString()));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.menu_new_host, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        switch (id)
        {
            case R.id.menu_new_host_confirm:
                saveHostToDatabase();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void saveHostToDatabase()
    {
        Host host = new Host(
            mHostId,
            true,
            mEditTextName.getText().toString(),
            mEditTextUrl.getText().toString(),
            mEditTextLogin.getText().toString(),
            mEditTextPassword.getText().toString(),
            ((SiteAPI)(mSpinnerSiteAPI.getSelectedItem())).getApiId(),
            Integer.parseInt(mSpinnerPageLimitsStrict.getSelectedItem().toString()),
            Integer.parseInt(mSpinnerPageLimitsRelaxed.getSelectedItem().toString())
        );

        try
        {
            HostsTable.addOrUpdateHost(host);
            Intent intent = new Intent(getActivity(), PostListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            getActivity().navigateUpTo(intent);
        }
        catch (IllegalArgumentException ex)
        {
            Toast.makeText(getActivity(), ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
