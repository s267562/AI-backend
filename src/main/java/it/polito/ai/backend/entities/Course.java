package it.polito.ai.backend.entities;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Course {
    @Id
    @EqualsAndHashCode.Include
    String id;
    String name;
    int min;
    int max;
    boolean enabled;

    @ManyToMany(mappedBy = "courses", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    List<Student> students = new ArrayList<>();

    public void addStudent(Student student) {
        students.add(student);
        student.courses.add(this);
    }

    public void removeStudent(Student student) {
        students.remove(student);
        student.courses.remove(this);
    }

    public void removeStudents() {
        for (Student s : students) {
            s.courses.remove(this);
        }
        students.clear();
    }

    @ManyToMany(mappedBy = "courses", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    List<Teacher> teachers = new ArrayList<>();

    public void addTeacher(Teacher teacher) {
        teachers.add(teacher);
        teacher.courses.add(this);
    }

    public void removeTeacher(Teacher teacher) {
        teachers.remove(teacher);
        teacher.courses.remove(this);
    }

    public void removeTeachers() {
        for (Teacher t : teachers) {
            t.courses.remove(this);
        }
        teachers.clear();
    }

    @OneToMany(mappedBy = "course", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    List<Team> teams = new ArrayList<>();

    public void addTeam(Team team) {
        team.course = this;
        teams.add(team);
    }

    public void removeTeam(Team team) {
        team.course = null;
        teams.remove(team);
    }

    public void removeTeams() {
        for (Team t : teams) {
            t.course = null;
            t.setConfiguration(null);
            t.removeMembers();
            t.removeVirtualMachines();
        }
        teams.clear();
    }

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @JoinColumn(name = "model_id")
    VirtualMachineModel virtualMachineModel;

    public void setVirtualMachineModel(VirtualMachineModel virtualMachineModel) {
        if (this.virtualMachineModel != null) {
            this.virtualMachineModel.course = null;
        }
        this.virtualMachineModel = virtualMachineModel;
        if (virtualMachineModel != null) {
            virtualMachineModel.course = this;
        }
    }


    @OneToMany(mappedBy = "course", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Assignment> assignments = new ArrayList<Assignment>();

    public void addAssignment(Assignment assignment) {
        assignment.course = this;
        assignments.add(assignment);
    }

    public void removeAssignment(Assignment assignment) {
        assignments.remove(assignment);
        assignment.course = null;
    }

    public void removeAssignments() {
        for (Assignment a : assignments) {
            a.course = null;
            a.removePapers();
        }
        assignments.clear();
    }

}
