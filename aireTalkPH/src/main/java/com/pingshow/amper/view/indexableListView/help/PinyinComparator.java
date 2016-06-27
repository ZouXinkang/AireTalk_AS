package com.pingshow.amper.view.indexableListView.help;




import com.pingshow.amper.view.indexableListView.IndexEntity;

import java.util.Comparator;

/**
 * Created by YoKeyword on 16/3/20.
 */
public class PinyinComparator<T extends IndexEntity> implements Comparator<T> {

    @Override
    public int compare(T lhs, T rhs) {
        return lhs.getFirstSpell().compareTo(rhs.getFirstSpell());
    }
}