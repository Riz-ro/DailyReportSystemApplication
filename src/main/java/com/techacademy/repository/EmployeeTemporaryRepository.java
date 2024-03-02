package com.techacademy.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.techacademy.entity.EmployeeTemporary;

public interface EmployeeTemporaryRepository extends JpaRepository<EmployeeTemporary, String> {
}