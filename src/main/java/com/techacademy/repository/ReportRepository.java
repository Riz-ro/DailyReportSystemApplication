package com.techacademy.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import com.techacademy.entity.Employee;
import com.techacademy.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Integer> {
    Page<Report> findAllByOrderByReportDateDesc(Pageable pageable);
    List<Report> findByEmployee(Employee employee);
    List<Report> findByEmployeeOrderByReportDateDesc(Employee employee);
    Page<Report> findByEmployeeOrderByReportDateDesc(Employee employee, Pageable pageable);
    boolean existsByEmployeeAndReportDate(Employee employee, LocalDate reportDate);
}