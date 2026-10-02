package com.school;

import jakarta.persistence.*;
import org.hibernate.annotations.SortNatural;

import java.util.*;

/** A student and their marks (each subject is out of 100), stored in the database. */
@Entity
public class Student {

    public static final double WEAK_BELOW = 50;   // subject counts as weak below this
    public static final double PASS_MARK = 40;    // overall percentage needed to pass

    @Id private String id;
    private String name;
    private String section;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "student_marks", joinColumns = @JoinColumn(name = "student_id"))
    @MapKeyColumn(name = "subject")
    @Column(name = "mark")
    @SortNatural
    private SortedMap<String, Double> marks = new TreeMap<>();

    protected Student() {}

    public Student(String id, String name, String section, Map<String, Double> marks) {
        this.id = id; this.name = name; this.section = section;
        this.marks.putAll(marks);
    }

    public String id()      { return id; }
    public String name()    { return name; }
    public String section() { return section; }
    public Map<String, Double> marks() { return marks; }
    public List<String> subjects() { return new ArrayList<>(marks.keySet()); }
    public List<Double> scores()   { return new ArrayList<>(marks.values()); }

    public double total()      { return marks.values().stream().mapToDouble(d -> d).sum(); }
    public int maxTotal()      { return marks.size() * 100; }
    public double average()    { return marks.isEmpty() ? 0 : total() / marks.size(); }
    public double percentage() { return maxTotal() == 0 ? 0 : total() * 100 / maxTotal(); }
    public String grade()      { return gradeFor(percentage()); }
    public boolean passed()    { return percentage() >= PASS_MARK; }

    public String gradeFor(double p) {
        return p >= 90 ? "A+" : p >= 80 ? "A" : p >= 70 ? "B" : p >= 60 ? "C" : p >= 50 ? "D" : p >= 40 ? "E" : "F";
    }

    public List<String> weakSubjects() {
        return marks.entrySet().stream().filter(e -> e.getValue() < WEAK_BELOW).map(Map.Entry::getKey).toList();
    }

    public record Focus(String subject, double marks, double target, double gap, int timeShare, String tip) {}

    /** Advice for a single subject mark. */
    public String tip(double m) {
        return m < 40 ? "Urgent: go back to the basics, practise a little every day and ask your teacher for help."
             : m < 50 ? "Needs extra time: work through past questions and fix the mistakes you keep repeating."
             : m < 70 ? "Getting there: regular practice can move this up a grade."
             : m < 85 ? "Good: keep revising to stay consistent."
             : "Excellent: maintain it, and try explaining topics to classmates.";
    }

    /** Subjects below 75, weakest first. Time share is proportional to how far each is from 75. */
    public List<Focus> focusPlan() {
        double[] steps = {40, 50, 60, 70, 80, 90, 100};
        double totalGap = marks.values().stream().filter(v -> v < 75).mapToDouble(v -> 75 - v).sum();
        if (totalGap == 0) return List.of();
        return marks.entrySet().stream().filter(e -> e.getValue() < 75)
                .sorted(Map.Entry.comparingByValue())
                .map(e -> {
                    double m = e.getValue(), t = 100;
                    for (double st : steps) { if (st > m) { t = st; break; } }
                    return new Focus(e.getKey(), m, t, t - m, (int) Math.round((75 - m) * 100 / totalGap), tip(m));
                }).toList();
    }

    public String insight() {
        if (marks.isEmpty()) return "";
        var best = Collections.max(marks.entrySet(), Map.Entry.comparingByValue());
        var worst = Collections.min(marks.entrySet(), Map.Entry.comparingByValue());
        if (best.getKey().equals(worst.getKey())) return "";
        return "Strongest subject: " + best.getKey() + " (" + Math.round(best.getValue()) + "). Weakest: "
             + worst.getKey() + " (" + Math.round(worst.getValue()) + "). Extra effort on "
             + worst.getKey() + " will lift the overall result the most.";
    }
}
