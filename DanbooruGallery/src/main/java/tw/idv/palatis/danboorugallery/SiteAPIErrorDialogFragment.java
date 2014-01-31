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

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import tw.idv.palatis.danboorugallery.siteapi.SiteAPI;

/**
 * Created by 其威 on 2014/1/30.
 */
public class SiteAPIErrorDialogFragment
    extends DialogFragment
{
    public static final String TAG = "SiteAPIErrorDialogFragment";

    public static final String KEY_TITLE = "title";
    public static final String KEY_URL = "url";
    public static final String KEY_MESSAGE = "message";

    SiteAPI.SiteAPIException mSiteAPIException;

    public SiteAPIErrorDialogFragment()
    {
        mSiteAPIException = null;
    }

    public SiteAPIErrorDialogFragment(SiteAPI.SiteAPIException exception)
    {
        mSiteAPIException = exception;
    }

    private String mTitle;
    private TextView mUrlText;
    private TextView mMessageText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_dialog_error_siteapi, container);

        mUrlText = (TextView) view.findViewById(R.id.text_url);
        mMessageText = (TextView) view.findViewById(R.id.text_messgae);

        if (savedInstanceState != null)
        {
            getDialog().setTitle(savedInstanceState.getString(KEY_TITLE));
            mUrlText.setText(savedInstanceState.getString(KEY_URL));
            mMessageText.setText(savedInstanceState.getString(KEY_MESSAGE));
        }
        else if (mSiteAPIException != null)
        {
            mTitle = getResources().getString(R.string.dialog_error_siteapi, mSiteAPIException.getSiteAPI().getName());
            getDialog().setTitle(mTitle);
            mUrlText.setText(mSiteAPIException.getUrl());
            mMessageText.setText(mSiteAPIException.getBody());
        }

        Button button = (Button) view.findViewById(android.R.id.button1);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                dismiss();
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (mSiteAPIException == null)
        {
            outState.putString(KEY_TITLE, mTitle);
            outState.putString(KEY_URL, mUrlText.getText().toString());
            outState.putString(KEY_MESSAGE, mMessageText.getText().toString());
        }
        else
        {
            outState.putString(KEY_TITLE, getResources().getString(
                R.string.dialog_error_siteapi, mSiteAPIException.getSiteAPI().getName()));
            outState.putString(KEY_URL, mSiteAPIException.getUrl());
            outState.putString(KEY_MESSAGE, mSiteAPIException.getBody());
        }
    }
}
