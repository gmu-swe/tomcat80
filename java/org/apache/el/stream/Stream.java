/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.el.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.el.LambdaExpression;

import org.apache.el.lang.ELSupport;

public class Stream {

    private final Iterator<Object> iterator;


    public Stream(Iterator<Object > iterator) {
        this.iterator = iterator;
    }


    public Stream filter(final LambdaExpression le) {
        Iterator<Object> downStream = new OpIterator() {
            @Override
            protected void findNext() {
                while (iterator.hasNext()) {
                    Object obj = iterator.next();
                    if (ELSupport.coerceToBoolean(
                            le.invoke(obj)).booleanValue()) {
                        next = obj;
                        foundNext = true;
                        break;
                    }
                }
            }
        };
        return new Stream(downStream);
    }


    public Stream map(final LambdaExpression le) {
        Iterator<Object> downStream = new OpIterator() {
            @Override
            protected void findNext() {
                if (iterator.hasNext()) {
                    Object obj = iterator.next();
                    next = le.invoke(obj);
                    foundNext = true;
                }
            }
        };
        return new Stream(downStream);
    }


    public Stream flatMap(final LambdaExpression le) {
        Iterator<Object> downStream = new OpIterator() {

            private Iterator<?> inner;

            @Override
            protected void findNext() {
                while (iterator.hasNext() ||
                        (inner != null && inner.hasNext())) {
                    if (inner == null || !inner.hasNext()) {
                        inner = ((Stream) le.invoke(iterator.next())).iterator;
                    }

                    if (inner.hasNext()) {
                        next = inner.next();
                        foundNext = true;
                        break;
                    }
                }
            }
        };
        return new Stream(downStream);
    }


    public Stream distinct() {
        Iterator<Object> downStream = new OpIterator() {

            private Set<Object> values = new HashSet<>();

            @Override
            protected void findNext() {
                while (iterator.hasNext()) {
                    Object obj = iterator.next();
                    if (values.add(obj)) {
                        next = obj;
                        foundNext = true;
                        break;
                    }
                }
            }
        };
        return new Stream(downStream);
    }


    public Stream sorted() {
        Iterator<Object> downStream = new OpIterator() {

            private Iterator<Object> sorted = null;

            @Override
            protected void findNext() {
                if (sorted == null) {
                    sort();
                }
                if (sorted.hasNext()) {
                    next = sorted.next();
                    foundNext = true;
                }
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            private final void sort() {
                List list = new ArrayList<>();
                while (iterator.hasNext()) {
                    list.add(iterator.next());
                }
                Collections.sort(list);
                sorted = list.iterator();
            }
        };
        return new Stream(downStream);
    }


    public Stream sorted(final LambdaExpression le) {
        Iterator<Object> downStream = new OpIterator() {

            private Iterator<Object> sorted = null;

            @Override
            protected void findNext() {
                if (sorted == null) {
                    sort(le);
                }
                if (sorted.hasNext()) {
                    next = sorted.next();
                    foundNext = true;
                }
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            private final void sort(LambdaExpression le) {
                List list = new ArrayList<>();
                Comparator<Object> c = new LambdaExpressionComparator(le);
                while (iterator.hasNext()) {
                    list.add(iterator.next());
                }
                Collections.sort(list, c);
                sorted = list.iterator();
            }
        };
        return new Stream(downStream);
    }


    public Object forEach(final LambdaExpression le) {
        while (iterator.hasNext()) {
            le.invoke(iterator.next());
        }
        return null;
    }


    public Stream peek(final LambdaExpression le) {
        Iterator<Object> downStream = new OpIterator() {
            @Override
            protected void findNext() {
                if (iterator.hasNext()) {
                    Object obj = iterator.next();
                    le.invoke(obj);
                    next = obj;
                    foundNext = true;
                }
            }
        };
        return new Stream(downStream);
    }


    public Iterator<?> iterator() {
        return iterator;
    }


    public Stream limit(final Number count) {
        return substream(Integer.valueOf(0), count);
    }


    public Stream substream(final Number start) {
        return substream(start, Integer.valueOf(Integer.MAX_VALUE));
    }

    public Stream substream(final Number start, final Number end) {

        Iterator<Object> downStream = new OpIterator() {

            private final int startPos = start.intValue();
            private final int endPos = end.intValue();
            private int itemCount = 0;

            @Override
            protected void findNext() {
                while (itemCount < startPos && iterator.hasNext()) {
                    iterator.next();
                    itemCount++;
                }
                if (itemCount < endPos && iterator.hasNext()) {
                    itemCount++;
                    next = iterator.next();
                    foundNext = true;
                }
            }
        };
        return new Stream(downStream);
    }


    public List<Object> toList() {
        List<Object> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }


    public Object[] toArray() {
        List<Object> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result.toArray(new Object[result.size()]);
    }


    public Optional reduce(LambdaExpression le) {
        Object seed = null;

        if (iterator.hasNext()) {
            seed = iterator.next();
        }

        if (seed == null) {
            return Optional.EMPTY;
        } else {
            return new Optional(reduce(seed, le));
        }
    }


    public Object reduce(Object seed, LambdaExpression le) {
        Object result = seed;

        while (iterator.hasNext()) {
            result = le.invoke(result, iterator.next());
        }

        return result;
    }


    private static class LambdaExpressionComparator
            implements Comparator<Object>{

        private final LambdaExpression le;

        public LambdaExpressionComparator(LambdaExpression le) {
            this.le = le;
        }

        @Override
        public int compare(Object o1, Object o2) {
            return ELSupport.coerceToNumber(
                    le.invoke(o1, o2), Integer.class).intValue();
        }
    }


    private abstract static class OpIterator implements Iterator<Object> {
        protected boolean foundNext = false;
        protected Object next;

        @Override
        public boolean hasNext() {
            if (foundNext) {
                return true;
            }
            findNext();
            return foundNext;
        }

        @Override
        public Object next() {
            if (foundNext) {
                foundNext = false;
                return next;
            }
            findNext();
            if (foundNext) {
                foundNext = false;
                return next;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        protected abstract void findNext();
    }
}
