package it.polito.ai.backend.entities;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
public class VirtualMachine {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    Long id;
    int num_vcpu;
    int disk_space;
    int ram;
    VirtualMachineStatus status;

    @ManyToMany(mappedBy = "virtual_machines")
    List<Student> owners = new ArrayList<>();

    public void addOwner(Student s) {
        owners.add(s);
        s.virtual_machines.add(this);
    }

    public void removeOwner(Student s) {
        owners.remove(s);
        s.virtual_machines.remove(this);
    }

    /*@ManyToOne
    @JoinColumn(name = "course_name")
    Course course;*/

    @ManyToOne
    @JoinColumn(name = "team_id")
    Team team;

    public void setTeam(Team team) {
        if (this.team != null) {
            this.team.virtual_machines.remove(this);
        }
        this.team = team;
        if (team != null) {
            team.virtual_machines.add(this);
        }
    }
}