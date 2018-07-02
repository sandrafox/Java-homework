package ru.ifmo.rain.lisicyna.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentQuery {
    private List<String> getList(List<Student> students, Function<Student, String> function) {
        return students.stream().map(function).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getList(students, (Student) -> Student.getFirstName() + " " + Student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return new TreeSet<String>((students.stream().map(Student::getFirstName).collect(Collectors.toSet())));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    private List<Student> getSortedList(Stream<Student> students, Comparator<Student> comparator) {
        return students.sorted(comparator).collect(Collectors.toList());
    }

    private Stream<Student> getFiltredStream(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream().filter(predicate);
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return getSortedList(students.stream(), Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return getSortedList(students.stream(), Comparator.comparing(Student::getLastName).thenComparing(
                Student::getFirstName).thenComparing(Student::compareTo));
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return getSortedList(getFiltredStream(students, Student -> Student.getFirstName() == name),
                Comparator.comparing(Student::getLastName).thenComparing(Student::compareTo));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return getSortedList(getFiltredStream(students, Student -> Student.getLastName() == name),
                Comparator.comparing(Student::getFirstName).thenComparing(Student::compareTo));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return getSortedList(getFiltredStream(students, Student -> Student.getGroup() == group),
                Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName).thenComparing(
                        Student::compareTo));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return getFiltredStream(students, Student -> Student.getGroup() == group).collect(
                Collectors.toMap(Student::getLastName, Student::getFirstName,
                        (s, a) -> s.compareTo(a) < 0 ? s : a));
    }
}
