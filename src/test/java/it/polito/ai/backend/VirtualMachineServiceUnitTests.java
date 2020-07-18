package it.polito.ai.backend;

import it.polito.ai.backend.dtos.VirtualMachineDTO;
import it.polito.ai.backend.entities.*;
import it.polito.ai.backend.repositories.*;
import it.polito.ai.backend.services.vm.VirtualMachineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc(addFilters = false)
class VirtualMachineServiceUnitTests {

    static List<Team> teams;
    static List<VirtualMachine> virtualMachines;
    static List<VirtualMachineConfiguration> configurations;
    static List<VirtualMachineModel> models;
    static List<Student> students;
    static List<Course> courses;

    @Autowired TeamRepository teamRepository;
    @Autowired VirtualMachineRepository virtualMachineRepository;
    @Autowired VirtualMachineConfigurationRepository virtualMachineConfigurationRepository;
    @Autowired VirtualMachineModelRepository virtualMachineModelRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired CourseRepository courseRepository;

    @BeforeAll
    static void beforeAll(@Autowired TeamRepository teamRepository,
                          @Autowired VirtualMachineRepository virtualMachineRepository,
                          @Autowired VirtualMachineConfigurationRepository virtualMachineConfigurationRepository,
                          @Autowired VirtualMachineModelRepository virtualMachineModelRepository,
                          @Autowired StudentRepository studentRepository,
                          @Autowired CourseRepository courseRepository) {

        if (virtualMachineRepository.count() > 0 &&
            teamRepository.count() > 0 &&
            virtualMachineConfigurationRepository.count() > 0 &&
            virtualMachineModelRepository.count() > 0 &&
            studentRepository.count() > 0 &&
            courseRepository.count() > 0)
        {

            teams = teamRepository.findAll();
            virtualMachines = virtualMachineRepository.findAll();
            configurations = virtualMachineConfigurationRepository.findAll();
            models = virtualMachineModelRepository.findAll();
            students = studentRepository.findAll();
            courses = courseRepository.findAll();
            return;
        }


        teams = new ArrayList<>();
        virtualMachines = new ArrayList<>();
        configurations = new ArrayList<>();
        models = new ArrayList<>();
        students = new ArrayList<>();
        courses = new ArrayList<>();

        final int nCourses = 6;
        final int nStudents = 50;
        final int nTeams = 20;

        virtualMachineModelRepository.deleteAll();
        virtualMachineConfigurationRepository.deleteAll();
        virtualMachineRepository.deleteAll();
        teamRepository.deleteAll();
        studentRepository.deleteAll();
        courseRepository.deleteAll();

        IntStream.range(0, nCourses)
                .forEach(i -> {
                    Course course = Course.builder()
                            .name("course-" + i)
                            .min(4)
                            .max(6)
                            .enabled(true)
                            .students(new ArrayList<>())
                            .teachers(new ArrayList<>())
                            .teams(new ArrayList<>())
                            .build();
                    courses.add(course);

                    VirtualMachineModel model = VirtualMachineModel.builder()
                            .system_image(SystemImage.values()[i%SystemImage.values().length])
                            .virtualMachines(new ArrayList<>())
                            .build();
                    models.add(model);

                    course.setVirtualMachineModel(model);

                });

        IntStream.range(0, nStudents)
                .forEach(i -> {
                    String id = "s-"+i;
                    Student student = Student.builder()
                            .id(id)
                            .name("name-" + id)
                            .firstName("first_name-" + id)
                            .courses(new ArrayList<>())
                            .teams(new ArrayList<>())
                            .virtual_machines(new ArrayList<>())
                            .build();
                    students.add(student);

                    courses.forEach(student::addCourse);
                });

        IntStream.range(0, nTeams)
                .forEach(i -> {
                    Team team = Team.builder()
                            .name("team-"+i)
                            .status(TeamStatus.ACTIVE)
                            .members(new ArrayList<>())
                            .virtualMachines(new ArrayList<>())
                            .build();
                    teams.add(team);

                    team.setCourse(courses.get(i%nCourses));

                    VirtualMachineConfiguration configuration = VirtualMachineConfiguration.builder()
                            .tot(5 + i%4)
                            .max_on(6 + i%3)
                            .min_vcpu(2)
                            .max_vcpu(12 + i%4)
                            .min_disk_space(300)
                            .max_disk_space(3000 + (i%3)*500)
                            .min_ram(4)
                            .max_ram(24)
                            .build();
                    configurations.add(configuration);

                    team.setVirtualMachineConfiguration(configuration);

                });

        IntStream.range(0, nStudents)
                .forEach(i -> {
                    Student student = students.get(i);
                    student.getCourses().forEach(c -> {
                        int size = c.getTeams().size();
                        Team team = c.getTeams().get(i%size);
                        if (team.getMembers().size() < c.getMin()) {
                            team.addStudent(student);

                            VirtualMachineConfiguration configuration = team.getVirtualMachineConfiguration();

                            if (team.getVirtualMachines().size() == 0) {

                                IntStream.range(0, configuration.getTot()-3)
                                        .forEach(j -> {
                                            VirtualMachine virtualMachine = VirtualMachine.builder()
                                                    .num_vcpu(configuration.getMin_vcpu())
                                                    .disk_space(configuration.getMin_disk_space())
                                                    .ram(configuration.getMin_ram())
                                                    .status(VirtualMachineStatus.OFF)
                                                    .owners(new ArrayList<>())
                                                    .build();

                                            virtualMachine.addOwner(student);
                                            virtualMachine.setTeam(team);
                                            virtualMachine.setVirtualMachineModel(c.getVirtualMachineModel());
                                            virtualMachines.add(virtualMachine);
                                        });

                                /*IntStream.range(0, configuration.getMax_on()-1)
                                        .forEach(k -> {
                                            team.getVirtualMachines().get(k%team.getVirtualMachines().size()).setStatus(VirtualMachineStatus.ON);
                                        });*/
                            }

                        }
                    });
                });

        teams.forEach(teamRepository::saveAndFlush);

    }

    @Autowired
    VirtualMachineService virtualMachineService;

    @Test
    void createVirtualMachine() {

        Team team = teams.get(0);
        Long teamId = team.getId();
        Student student = team.getMembers().get(0);
        String studentId = student.getId();
        VirtualMachineConfiguration virtualMachineConfiguration = team.getVirtualMachineConfiguration();
        VirtualMachineModel virtualMachineModel = team.getCourse().getVirtualMachineModel();
        int vcpu = virtualMachineConfiguration.getMin_vcpu();
        int diskSpace = virtualMachineConfiguration.getMin_disk_space();
        int ram = virtualMachineConfiguration.getMin_ram();

        VirtualMachineDTO virtualMachineDTO = virtualMachineService.createVirtualMachine(studentId, teamId, vcpu, diskSpace, ram);

        Assertions.assertTrue(virtualMachineDTO.getId() > 0);

        VirtualMachine virtualMachine = virtualMachineRepository.getOne(virtualMachineDTO.getId());
        Assertions.assertEquals(virtualMachine.getId(), virtualMachineDTO.getId());
        Assertions.assertSame(virtualMachine.getStatus(), VirtualMachineStatus.OFF);
        Assertions.assertEquals(virtualMachine.getNum_vcpu(), vcpu);
        Assertions.assertEquals(virtualMachine.getDisk_space(), diskSpace);
        Assertions.assertEquals(virtualMachine.getRam(), ram);
        Assertions.assertEquals(virtualMachine.getTeam(), team);
        Assertions.assertEquals(virtualMachine.getVirtualMachineModel(), virtualMachineModel);
        Assertions.assertTrue(virtualMachine.getOwners().contains(students.get(0)));

    }

    @Test
    void updateVirtualMachine() {

        VirtualMachine virtualMachine = virtualMachines.get(0);

        VirtualMachineDTO virtualMachineDTO = VirtualMachineDTO.builder()
                .id(virtualMachine.getId())
                .num_vcpu(virtualMachine.getNum_vcpu()+1)
                .disk_space(virtualMachine.getDisk_space()+1)
                .ram(virtualMachine.getRam()+1)
                .build();

        VirtualMachineDTO updated = virtualMachineService.updateVirtualMachine(virtualMachine.getId(), virtualMachineDTO);

        Assertions.assertEquals(virtualMachineDTO.getNum_vcpu(), updated.getNum_vcpu());
        Assertions.assertEquals(virtualMachineDTO.getDisk_space(), updated.getDisk_space());
        Assertions.assertEquals(virtualMachineDTO.getRam(), updated.getRam());
        Assertions.assertEquals(virtualMachineDTO.getId(), updated.getId());
    }

    @Test
    void getActiveVcpuForTeam() {
        Team team = teams.get(0);
        long activeVcpu = team.getVirtualMachines()
                .stream()
                .filter(vm -> vm.getStatus() == VirtualMachineStatus.ON)
                .reduce(0, (partial, current) -> partial + current.getNum_vcpu(), Integer::sum);

        Assertions.assertEquals(activeVcpu, virtualMachineService.getActiveVcpuForTeam(team.getId()));

    }

    @Test
    void getCountActiveVMsForTeam() {
        Team team = teams.get(0);
        long activeVMs = team.getVirtualMachines()
                .stream()
                .filter(vm -> vm.getStatus() == VirtualMachineStatus.ON)
                .count();

        Assertions.assertEquals(activeVMs, virtualMachineService.getCountActiveVirtualMachinesForTeam(team.getId()));
    }

    @Test
    void getCountVMsForTeam() {
        Team team = teams.get(0);
        long count = team.getVirtualMachines().size();

        Assertions.assertEquals(count, virtualMachineService.getCountVirtualMachinesForTeam(team.getId()));
    }

    /*@Test
    void contextLoad() {

    }*/

}