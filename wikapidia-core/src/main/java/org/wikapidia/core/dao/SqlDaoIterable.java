package org.wikapidia.core.dao;

import org.jooq.Cursor;
import org.jooq.Record;
import java.lang.UnsupportedOperationException;
import java.util.Iterator;

public abstract class SqlDaoIterable<E> implements Iterable<E> {
    Cursor<Record> result;

    private boolean usedUp = false;

    public SqlDaoIterable(Cursor<Record> result){
        this.result=result;
    }

    public abstract E transform(Record record);

    @Override
    public Iterator<E> iterator() {
        if (usedUp) {
            throw new IllegalStateException(
                    "SqlDaoIterable can only be iterated over once. " +
                    "We should change this to an iterator but for-each loops are nice."
            );
        }
        usedUp = true;
        return new Iterator<E>() {
            Iterator<Record> recordIterator = result.iterator();

            @Override
            public boolean hasNext() {
                return recordIterator.hasNext();
            }

            @Override
            public E next() {
                return transform(recordIterator.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}