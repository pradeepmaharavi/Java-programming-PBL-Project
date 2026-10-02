package com.school;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, String> {

    @Query("select s from Student s where lower(s.id) = :k or lower(s.section) = :k "
         + "or lower(s.name) like concat('%', :k, '%') order by lower(s.name)")
    List<Student> search(@Param("k") String k);

    List<Student> findBySectionIgnoreCase(String section);
}
