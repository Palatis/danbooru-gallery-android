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

package tw.idv.palatis.danboorugallery.siteapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;

public class SiteAPIException
    extends IOException
{
    protected final int mResponseCode;
    protected final String mResponseMessage;
    protected final SiteAPI mSiteAPI;
    protected final String mUrl;
    protected final String mBody;

    public SiteAPIException()
    {
        this("");
    }

    public SiteAPIException(String message)
    {
        this(message, null);
    }

    public SiteAPIException(Throwable cause)
    {
        this("", cause);
    }

    public SiteAPIException(String message, Throwable cause)
    {
        super(message, cause);

        mSiteAPI = null;
        mResponseCode = -1;
        mResponseMessage = mUrl = mBody = "";
    }

    public SiteAPIException(SiteAPI api, HttpURLConnection connection, Throwable cause)
    {
        super(cause);
        mSiteAPI = api;
        int responseCode = -1;
        String responseMessage = "", url = "", body = "";
        if (connection != null)
        {
            url = connection.getURL().toString();
            try { responseCode = connection.getResponseCode(); } catch (IOException ignore) { }
            try { responseMessage = connection.getResponseMessage(); } catch (IOException ignore) { }
            try
            {
                Reader input = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
                Writer output = new StringWriter();
                char buffer[] = new char[1024];
                for (int count = input.read(buffer);count > 0;count = input.read(buffer))
                    output.write(buffer, 0, count);

                body = output.toString();
            }
            catch (NullPointerException | IOException ignored) { }
        }
        mUrl = url;
        mBody = body;
        mResponseCode = responseCode;
        mResponseMessage = responseMessage;
    }

    @Override
    public String getMessage()
    {
        if (mSiteAPI == null)
            return super.getMessage();
        return mSiteAPI.getName() + " [" + mUrl + "]: " + mResponseCode + " " + mResponseMessage + ": " + mBody;
    }

    @Override
    public String getLocalizedMessage()
    {
        if (mSiteAPI == null)
            return super.getLocalizedMessage();
        return mSiteAPI.getName() + " [" + mUrl + "]: " + mResponseCode + " " + mResponseMessage + ": " + mBody;
    }

    public int getResponseCode()
    {
        return mResponseCode;
    }

    public String getResponseMessage()
    {
        return mResponseMessage;
    }

    public SiteAPI getSiteAPI()
    {
        return mSiteAPI;
    }

    public String getUrl()
    {
        return mUrl;
    }

    public String getBody()
    {
        return mBody;
    }
}