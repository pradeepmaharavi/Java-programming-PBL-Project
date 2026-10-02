package com.school;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Controller
public class StudentController {

    public record Row(String subject, String mark) {}
    public record FormData(String id, String name, String section, List<Row> rows, boolean editing) {}

    private final StudentService service;
    public StudentController(StudentService service) { this.service = service; }

    private Student find(String id) {
        return service.find(id.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Dashboard: search box, class summary and student list (summary follows the search). */
    @GetMapping("/")
    public String home(@RequestParam(required = false) String q, Model m) {
        var list = service.search(q);
        m.addAttribute("students", list);
        m.addAttribute("sum", service.summarize(list));
        m.addAttribute("q", q == null ? "" : q);
        return "index";
    }

    @GetMapping("/new")
    public String form(Model m) {
        m.addAttribute("f", new FormData("", "", "",
                List.of(new Row("Maths", ""), new Row("Science", ""), new Row("English", "")), false));
        return "form";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model m) {
        Student s = find(id);
        var rows = s.marks().entrySet().stream().map(e -> new Row(e.getKey(), String.valueOf(e.getValue()))).toList();
        m.addAttribute("f", new FormData(s.id(), s.name(), s.section(), rows, true));
        return "form";
    }

    @PostMapping("/save")
    public String save(@RequestParam String id, @RequestParam String name, @RequestParam String section,
                       @RequestParam(defaultValue = "false") boolean editing,
                       @RequestParam(required = false) List<String> subject,
                       @RequestParam(required = false) List<String> mark, Model m) {
        id = id.trim().toUpperCase(); name = name.trim(); section = section.trim();
        List<String> errors = new ArrayList<>();
        List<Row> rows = new ArrayList<>();
        var marks = new TreeMap<String, Double>();
        var seen = new HashSet<String>();

        if (!id.matches("[A-Z0-9_-]{1,20}")) errors.add("ID must be 1 to 20 letters, digits, - or _ (no spaces).");
        else if (!editing && service.exists(id)) errors.add("ID " + id + " already exists. Use Edit on that student instead.");
        if (name.isEmpty()) errors.add("Name is required.");
        if (section.isEmpty()) errors.add("Class / section is required.");

        int n = subject == null ? 0 : subject.size();
        for (int i = 0; i < n; i++) {
            String sub = subject.get(i).trim();
            String raw = (mark != null && i < mark.size()) ? mark.get(i).trim() : "";
            rows.add(new Row(sub, raw));
            if (sub.isEmpty() && raw.isEmpty()) continue;
            if (sub.isEmpty()) { errors.add("A mark was entered without a subject name."); continue; }
            if (raw.isEmpty()) { errors.add("Enter a mark for " + sub + " or remove that subject."); continue; }
            double v;
            try { v = Double.parseDouble(raw); }
            catch (NumberFormatException e) { errors.add("The mark for " + sub + " must be a number."); continue; }
            if (Double.isNaN(v) || v < 0 || v > 100) { errors.add("The mark for " + sub + " must be between 0 and 100."); continue; }
            if (!seen.add(sub.toLowerCase())) { errors.add("Subject " + sub + " is entered twice."); continue; }
            marks.put(sub, v);
        }
        if (marks.isEmpty() && errors.isEmpty()) errors.add("Add at least one subject with a mark.");

        if (!errors.isEmpty()) {
            m.addAttribute("errors", errors);
            m.addAttribute("f", new FormData(id, name, section,
                    rows.isEmpty() ? List.of(new Row("", "")) : rows, editing));
            return "form";
        }
        service.save(new Student(id, name, section, marks));
        return "redirect:/student/" + id;
    }

    @GetMapping("/student/{id}")
    public String report(@PathVariable String id, Model m) {
        Student s = find(id);
        m.addAttribute("s", s);
        m.addAttribute("rank", service.rank(s));
        return "report";
    }

    /** The student names a topic they find hard; we return practice questions for it. */
    @GetMapping("/student/{id}/practice")
    public String practice(@PathVariable String id, @RequestParam String subject,
                           @RequestParam String topic, Model m) {
        Student s = find(id);
        Double mark = s.marks().get(subject);
        if (mark == null || topic.isBlank()) return "redirect:/student/" + s.id();
        m.addAttribute("s", s);
        m.addAttribute("subject", subject);
        m.addAttribute("topic", topic.trim());
        m.addAttribute("mark", mark);
        m.addAttribute("questions", Practice.questions(subject, topic.trim(), mark));
        return "practice";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable String id) { service.delete(id.trim().toUpperCase()); return "redirect:/"; }
}
