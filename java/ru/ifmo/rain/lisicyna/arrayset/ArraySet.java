package ru.ifmo.rain.lisicyna.arrayset;

import java.util.*;

public class ArraySet<E> implements SortedSet<E> {
    private List<E> objects;
    private Comparator<? super E> comparator = null;

    public ArraySet (List<E> collection, Comparator<? super E> comp, boolean sorted) {
        if (sorted) {
            objects = collection;
            comparator = comp;
        } else {
            comparator = comp;
            objects = new ArrayList<E>(collection);
            objects.sort(comp);
            notEqualsElement();
        }
    }

    public ArraySet (Collection<? extends E> collection, Comparator<? super E> comp) {
        comparator = comp;
        objects = new ArrayList<E>(collection);
        objects.sort(comp);
        notEqualsElement();
    }

    public ArraySet (Collection<? extends E> collection) {
        objects = new ArrayList<E>(collection);
        comparator = null;
        objects.sort(comparator);
        notEqualsElement();
    }

    public ArraySet () {
        objects = new ArrayList<E>(0);
        comparator = null;
    }

    private void  notEqualsElement() {
        if (comparator != null) {
            int i = 1;
            while (i < objects.size()) {
                if (comparator.compare(objects.get(i - 1), objects.get(i)) == 0) {
                    objects.remove(i);
                } else {
                    i++;
                }
            }
        } else {
            int i = 0;
            while (i < objects.size() - 1) {
                if (objects.get(i).equals(objects.get(i + 1))) {
                    objects.remove(i);
                } else {
                    i++;
                }
            }
        }
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public ArraySet<E> subSet(E fromElement, E toElement) {
        int index1 = Collections.binarySearch(objects, fromElement, comparator);
        int index2 = Collections.binarySearch(objects, toElement, comparator);
        index1 = index1 >= 0 ? index1 : -(index1 + 1);
        index2 = index2 >= 0 ? index2 : -(index2 + 1);
        return new ArraySet<E>(objects.subList(index1, index2), this.comparator, true);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        int index2 = Collections.binarySearch(objects, toElement, comparator);
        index2 = index2 >= 0 ? index2 : -(index2 + 1);
        return new ArraySet<E>(objects.subList(0, index2), this.comparator, true);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        int index1 = Collections.binarySearch(objects, fromElement, comparator);
        index1 = index1 >= 0 ? index1 : -(index1 + 1);
        return new ArraySet<E>(objects.subList(index1, objects.size()), this.comparator, true);
    }

    @Override
    public E first() {
        if (objects.size() == 0) {
            throw new NoSuchElementException("Sorry, this set is empty");
        }
        return objects.get(0);
    }

    @Override
    public E last() {
        if (objects.size() == 0) {
            throw new NoSuchElementException("Sorry, this set is empty");
        }
        return objects.get(objects.size() - 1);
    }

    @Override
    public int size() {
        return objects.size();
    }

    @Override
    public boolean isEmpty() {
        return objects.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(objects, (E) o, comparator) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(objects).iterator();
    }

    @Override
    public Object[] toArray() {
        return objects.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return objects.toArray(a);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("Sorry, this set is immutable");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Sorry, this set is immutable");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        boolean result = true;
        for (Object o : c) {
            result &= Collections.binarySearch(objects, (E) o, comparator) >= 0;
        }
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException("Sorry, this set is immutable");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Sorry, this set is immutable");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Sorry, this set is immutable");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Sorry, this set is immutable");
    }
}
