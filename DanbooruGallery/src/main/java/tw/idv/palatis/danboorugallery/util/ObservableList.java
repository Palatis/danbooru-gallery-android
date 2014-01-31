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

package tw.idv.palatis.danboorugallery.util;

import android.database.DataSetObservable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ObservableList<T>
    extends DataSetObservable
    implements List<T>
{
    private List<T> mList = null;

    public ObservableList(List<T> list)
    {
        mList = list;
    }

    public void sort(Comparator<T> comparator)
    {
        Collections.sort(mList, comparator);
        notifyChanged();
    }

    public void setList(List<T> list)
    {
        if (mList == list)
            return;

        mList = list;
        notifyChanged();
    }

    public List<T> getList()
    {
        return mList;
    }

    @Override
    public void add(int position, T item)
    {
        mList.add(position, item);
        notifyChanged();
    }

    @Override
    public boolean add(T item)
    {
        boolean result = mList.add(item);
        notifyChanged();
        return result;
    }

    @Override
    public boolean addAll(int position, Collection<? extends T> items)
    {
        boolean result = mList.addAll(position, items);
        notifyChanged();
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends T> items)
    {
        boolean result = mList.addAll(items);
        notifyChanged();
        return result;
    }

    @Override
    public void clear()
    {
        mList.clear();
        notifyInvalidated();
    }

    @Override
    public boolean contains(Object item)
    {
        return mList.contains(item);
    }

    @Override
    public boolean containsAll(Collection<?> items)
    {
        return mList.containsAll(items);
    }

    @Override
    public T get(int position)
    {
        return mList.get(position);
    }

    @Override
    public int indexOf(Object item)
    {
        return mList.indexOf(item);
    }

    @Override
    public boolean isEmpty()
    {
        return mList.isEmpty();
    }

    @Override
    public Iterator<T> iterator()
    {
        return mList.iterator();
    }

    @Override
    public int lastIndexOf(Object item)
    {
        return mList.lastIndexOf(item);
    }

    @Override
    public ListIterator<T> listIterator()
    {
        return mList.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int position)
    {
        return mList.listIterator(position);
    }

    @Override
    public T remove(int position)
    {
        T item = mList.remove(position);
        notifyChanged();
        return item;
    }

    @Override
    public boolean remove(Object item)
    {
        boolean result = mList.remove(item);
        notifyChanged();
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> items)
    {
        boolean result = mList.removeAll(items);
        notifyChanged();
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> list)
    {
        boolean result = mList.retainAll(list);
        notifyChanged();
        return false;
    }

    @Override
    public T set(int position, T item)
    {
        item = mList.set(position, item);
        notifyChanged();
        return item;
    }

    @Override
    public int size()
    {
        return mList.size();
    }

    @Override
    public List<T> subList(int from, int to)
    {
        return mList.subList(from, to);
    }

    @Override
    public Object[] toArray()
    {
        return mList.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] array)
    {
        return mList.toArray(array);
    }
}
