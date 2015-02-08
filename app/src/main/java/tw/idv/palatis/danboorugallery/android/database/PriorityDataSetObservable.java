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

package tw.idv.palatis.danboorugallery.android.database;

import android.database.DataSetObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link android.database.DataSetObservable} with respect to observer priority.
 */
@SuppressWarnings("unused")
public class PriorityDataSetObservable
{
    private static class Enclosure
        implements Comparable<Enclosure>
    {
        public int priority;
        public DataSetObserver observer;

        public Enclosure(int priority, DataSetObserver observer)
        {
            this.priority = priority;
            this.observer = observer;
        }

        @Override
        public int compareTo(Enclosure rhs)
        {
            // sort descending, because we iterate from last to first.
            return rhs.priority - priority;
        }
    }

    private boolean mUnsorted = false;
    private List<Enclosure> mObservers = new ArrayList<>();
    private int mDefaultPriority;

    /**
     * Creates an PriorityDataSetObservable with default priority.
     * @param defaultPriority
     *      the default priority of the observer when no priority specified.
     *      See also {@link tw.idv.palatis.danboorugallery.android.database.PriorityDataSetObservable#registerObserver(android.database.DataSetObserver)}
     */
    public PriorityDataSetObservable(int defaultPriority)
    {
        mDefaultPriority = defaultPriority;
    }

    /**
     * Creates an PriorityDataSetObserver, with default to least priority.
     */
    public PriorityDataSetObservable()
    {
        this(Integer.MAX_VALUE);
    }

    /**
     * get the default priority for newly registered observers
     * @return  the default priority
     */
    synchronized public int getDefaultPriority()
    {
        return mDefaultPriority;
    }

    /**
     * set the default priority for newly registered observers
     * @param defaultPriority   the new default priority
     */
    synchronized public void setDefaultPriority(int defaultPriority)
    {
        mDefaultPriority = defaultPriority;
    }

    /**
     * Register an observer with default priority.
     * @param observer    the observer to be registered
     */
    synchronized public void registerObserver(DataSetObserver observer)
    {
        registerObserver(observer, Integer.MAX_VALUE);
    }

    /**
     * Register an observer with priority, lower priority values has higher priority.
     * @param observer    the observer to be registered
     * @param priority    the priority, lower values means higher priority.
     */
    synchronized public void registerObserver(DataSetObserver observer, int priority)
    {
        mObservers.add(new Enclosure(priority, observer));
        mUnsorted = true;
    }

    /**
     * Unregister an observer, the one with highest priority (lowest priority value)
     * gets unregistered.
     * @param observer  the observer to be unregistered.
     */
    synchronized public void unregisterObserver(DataSetObserver observer)
    {
        for (int target = mObservers.size() - 1;target >= 0;--target)
            if (mObservers.get(target).observer == observer)
            {
                mObservers.remove(target);
                mUnsorted = true;
                break;
            }
    }

    /**
     * Flushes out everything
     */
    synchronized public void unregisterAll()
    {
        mObservers.clear();
        mUnsorted = false;
    }

    /**
     * notifies the observers that the data-set has changed
     */
    synchronized public void notifyChanged()
    {
        if (mUnsorted)
            Collections.sort(mObservers);

        for (int i = mObservers.size() - 1;i >= 0;--i)
            mObservers.get(i).observer.onChanged();
    }

    /**
     * notifies the observers that the data-set has invalidated
     */
    synchronized public void notifyInvalidated()
    {
        if (mUnsorted)
            Collections.sort(mObservers);

        for (int i = mObservers.size() - 1;i >= 0;--i)
            mObservers.get(i).observer.onInvalidated();
    }
}
