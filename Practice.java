package com.school;

import java.util.ArrayList;
import java.util.List;

/** Builds practice questions for a subject and topic. Lower marks get the basic questions first. */
public final class Practice {
    private Practice() {}

    private static final String PROBLEM_SUBJECTS = ".*(math|phys|chem|science|stat|account|comput|program|econ).*";

    public static List<String> questions(String subject, String topic, double mark) {
        String t = topic, s = subject;
        var q = new ArrayList<String>();
        if (subject.toLowerCase().matches(PROBLEM_SUBJECTS)) {
            q.add("In your own words, what is " + t + "? Write 2 to 3 sentences.");
            q.add("Write down the key formulas, rules or steps you need for " + t + ".");
            q.add("Solve one simple example on " + t + ". Show every step.");
            q.add("Which step in " + t + " do you find hardest, and why?");
            q.add("Take a question on " + t + " from your textbook and solve it without looking at the answer. Then check where you lost marks.");
            q.add("What mistake do students often make in " + t + ", and how can you avoid it?");
            if (mark >= Student.PASS_MARK) {
                q.add("Solve a mixed question that combines " + t + " with another topic in " + s + ".");
                q.add("Explain to a friend in 2 minutes how to solve " + t + " problems from start to finish.");
            }
        } else {
            q.add("Explain " + t + " in your own words in 3 to 4 sentences.");
            q.add("List the 5 most important points, terms or dates about " + t + ".");
            q.add("Give one example that shows " + t + ", and explain why it fits.");
            q.add("What is the most confusing part of " + t + "? Write one question you would ask your teacher.");
            q.add("Write a short exam-style answer (about 5 lines) on " + t + ".");
            q.add("What common mistake do students make when writing about " + t + "?");
            if (mark >= Student.PASS_MARK) {
                q.add("Compare " + t + " with a related idea in " + s + ". What is similar and what is different?");
                q.add("Why does " + t + " matter? Give a cause and an effect, or an argument for and against.");
            }
        }
        return q;
    }
}
