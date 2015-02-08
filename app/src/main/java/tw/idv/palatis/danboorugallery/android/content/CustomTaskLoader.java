/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tw.idv.palatis.danboorugallery.android.content;

import android.annotation.TargetApi;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;

/**
 * A loader that runs your callable and returns a result.
 * This class implements the {@link android.content.Loader} protocal in a
 * standard way for doing custom calls, building on
 * {@link android.content.AsyncTaskLoader} to perform the call on a background
 * thread so that it does not block the applications's UI.
 */
public abstract class CustomTaskLoader<T>
    extends AsyncTaskLoader<T>
{
    protected T mResult;
    protected CancellationSignal mCancellationSignal;

    /**
     * Let the task do it's job. Runs on a worker thread.
     * @param signal    the {@link android.os.CancellationSignal}
     * @return          the result
     */
    public abstract T runTaskInBackground(CancellationSignal signal);

    /**
     * Called when the old result is unneeded anymore. May run on the worker
     * thread or the UI thread.
     * @param oldResult    the old result
     */
    public abstract void cleanUp(T oldResult);

    /* Runs on a worker thread */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public T loadInBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            synchronized (this)
            {
                if (isLoadInBackgroundCanceled())
                    throw new OperationCanceledException();
                mCancellationSignal = new CancellationSignal();
            }
        }

        try
        {
            return runTaskInBackground(mCancellationSignal);
        }
        finally
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                synchronized (this)
                {
                    mCancellationSignal = null;
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void cancelLoadInBackground()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            super.cancelLoadInBackground();

            synchronized (this)
            {
                if (mCancellationSignal != null)
                    mCancellationSignal.cancel();
            }
        }
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(T result)
    {
        if (isReset())
        {
            // An async query came in while the loader is stopped
            if (result != null)
                cleanUp(result);
            return;
        }

        T oldResult = mResult;
        mResult = oldResult;

        if (isStarted())
            super.deliverResult(result);

        if (oldResult != null && oldResult != result)
            cleanUp(oldResult);
    }

    /**
     * Creates an empty unspecified CustomTaskLoader.
     */
    public CustomTaskLoader(Context context)
    {
        super(context);
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading()
    {
        if (takeContentChanged() || mResult == null)
            forceLoad();
        else
            deliverResult(mResult);
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading()
    {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(T result)
    {
        if (result != null)
            cleanUp(result);
    }

    @Override
    protected void onReset()
    {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        if (mResult != null)
            cleanUp(mResult);
        mResult = null;
    }
}