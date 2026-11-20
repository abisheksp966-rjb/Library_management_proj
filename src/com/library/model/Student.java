package com.library.model;

/**
 * Model representing a Student member in the college library system.
 */
public class Student {
    private int studentId;
    private String registerNo;
    private String name;
    private String department;
    private int year;
    private String email;

    public Student() {}

    public Student(int studentId, String registerNo, String name, String department, int year, String email) {
        this.studentId = studentId;
        this.registerNo = registerNo;
        this.name = name;
        this.department = department;
        this.year = year;
        this.email = email;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getRegisterNo() {
        return registerNo;
    }

    public void setRegisterNo(String registerNo) {
        this.registerNo = registerNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return String.format("[ID: %d] %s (%s) - Dept: %s, Year: %d", studentId, name, registerNo, department, year);
    }
}
