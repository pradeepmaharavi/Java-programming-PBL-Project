package com.school;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentService {

    public record Summary(int count, double average, Student highest, Student lowest, long passed,
                          Map<String, Double> subjectAverages, Map<String, Long> grades,
                          List<Student> top, Map<String, Long> weakCounts, String weakestSubject) {}

    public record Rank(int position, int of) {}

    private final StudentRepository repo;
    public StudentService(StudentRepository repo) { this.repo = repo; }

    /** Adds a few demo students the first time the database is empty. */
    @PostConstruct
    void seed() {
        if (repo.count() > 0) return;
        save(mk("S101", "Asha Raman", "10-A", "Maths", 92, "Science", 88, "English", 79, "History", 85));
        save(mk("S102", "Karthik Selvam", "10-A", "Maths", 45, "Science", 62, "English", 71, "History", 38));
        save(mk("S103", "Meena Iyer", "10-B", "Maths", 68, "Science", 55, "English", 82, "History", 74));
        save(mk("S104", "Rahul Das", "10-B", "Maths", 30, "Science", 42, "English", 58, "History", 65));
        save(mk("S105", "Priya Nair", "10-A", "Maths", 76, "Science", 81, "English", 64, "History", 52));
    }

    private Student mk(String id, String name, String sec, Object... kv) {
        var m = new TreeMap<String, Double>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        return new Student(id, name, sec, m);
    }

    public Student save(Student s)           { return repo.save(s); }   // same ID = update
    public void delete(String id)            { repo.deleteById(id); }
    public Optional<Student> find(String id) { return repo.findById(id); }
    public boolean exists(String id)         { return repo.existsById(id); }

    /** Matches exact ID, exact section, or part of the name. Blank query returns everyone. */
    public List<Student> search(String q) {
        String k = q == null ? "" : q.trim().toLowerCase();
        if (k.isEmpty()) {
            return repo.findAll().stream()
                    .sorted(Comparator.comparing(Student::name, String.CASE_INSENSITIVE_ORDER)).toList();
        }
        return repo.search(k);
    }

    /** Position among students of the same class/section (1 = best). */
    public Rank rank(Student s) {
        var mates = repo.findBySectionIgnoreCase(s.section());
        int pos = 1 + (int) mates.stream().filter(o -> o.percentage() > s.percentage()).count();
        return new Rank(pos, mates.size());
    }

    public Summary summarize(List<Student> list) {
        if (list.isEmpty()) return new Summary(0, 0, null, null, 0, Map.of(), Map.of(), List.of(), Map.of(), "");
        double avg = list.stream().mapToDouble(Student::percentage).average().orElse(0);
        Student hi = list.stream().max(Comparator.comparingDouble(Student::percentage)).get();
        Student lo = list.stream().min(Comparator.comparingDouble(Student::percentage)).get();
        long pass = list.stream().filter(Student::passed).count();
        Map<String, Double> subj = list.stream().flatMap(s -> s.marks().entrySet().stream())
                .collect(Collectors.groupingBy(e -> e.getKey(), TreeMap::new,
                        Collectors.averagingDouble(e -> e.getValue())));
        Map<String, Long> grades = list.stream()
                .collect(Collectors.groupingBy(Student::grade, TreeMap::new, Collectors.counting()));
        List<Student> top = list.stream()
                .sorted(Comparator.comparingDouble(Student::percentage).reversed()).limit(3).toList();
        Map<String, Long> weak = list.stream().flatMap(s -> s.marks().entrySet().stream())
                .filter(e -> e.getValue() < Student.WEAK_BELOW)
                .collect(Collectors.groupingBy(e -> e.getKey(), TreeMap::new, Collectors.counting()));
        String weakest = subj.entrySet().stream().min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("");
        return new Summary(list.size(), avg, hi, lo, pass, subj, grades, top, weak, weakest);
    }
}
