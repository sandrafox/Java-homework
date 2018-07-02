package ru.ifmo.rain.lisicyna.arrayset;

import java.util.ArrayList;

public class Testing {
    public static void main(String[] args) {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        arr.add(552056382);
        arr.add(-1624534014);
        arr.add(-83370781);
        arr.add(952129236);
        arr.add(-387136005);
        ArraySet<Integer> ar = new ArraySet<Integer>(arr);
        ArraySet<Integer> ar1 = ar.subSet(-83370781, 1687889663);
    }
}
