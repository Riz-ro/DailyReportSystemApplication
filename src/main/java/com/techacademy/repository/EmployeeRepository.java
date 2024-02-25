package com.techacademy.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.techacademy.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Page<Employee> findAll(Pageable pageable);
    Page<Employee> findAllByDeleteFlgFalse(Pageable pageable);
    List<Employee> findAllByDeleteFlgFalse();
    boolean existsByCode(String code);
}