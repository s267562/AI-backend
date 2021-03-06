package it.polito.ai.backend.security;

import it.polito.ai.backend.entities.Student;
import it.polito.ai.backend.entities.User;
import it.polito.ai.backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class SecurityServiceImpl implements SecurityService {

    @Autowired
    CourseRepository courseRepository;
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    TeacherRepository teacherRepository;
    @Autowired
    VirtualMachineRepository virtualMachineRepository;
    @Autowired
    VirtualMachineModelRepository virtualMachineModelRepository;
    @Autowired
    ConfigurationRepository configurationRepository;
    @Autowired
    AssignmentRepository exerciseRepository;
    @Autowired
    PaperRepository paperRepository;
    @Autowired
    TokenRepository tokenRepository;
    @Autowired
    StudentRepository studentRepository;


    /**
     *
     * @param id
     * @return true if id matches with the one of the authenticated user
     */
    @Override
    public boolean isAuthorized(String id) {
        String userId = this.getId();
        return userId.equalsIgnoreCase(id);
    }


    /**
     *
     * @param courseId
     * @return true if the authenticated student is enrolled in courseId
     */
    @Override
    public boolean isEnrolled(String courseId) {
        String userId = this.getId();
        return courseRepository.findById(courseId)
                .map(c -> c.getStudents()
                        .stream()
                        .anyMatch(s -> s.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }

    /**
     *
     * @param teamId
     * @return if the authenticated student is part of teamId
     */
    @Override
    public boolean isPartOf(Long teamId) {
        String userId = this.getId();
        return teamRepository.findById(teamId)
                .map(t -> t.getMembers()
                        .stream()
                        .anyMatch(s -> s.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }

    /**
     *
     * @param courseId
     * @return true if courseId is taught by the authenticated teacher
     */
    @Override
    public boolean isTaught(String courseId) {
        String userId = this.getId();
        return courseRepository.findById(courseId)
                .map(c -> c.getTeachers()
                        .stream()
                        .anyMatch(t -> t.getId().equalsIgnoreCase(userId)))
                .orElse(false);

    }


    /**
     *
     * @param configurationId
     * @return true if the course of the team for which the configurationId has been defined
     * is taught by the authenticated teacher
     */
    @Override
    public boolean canManage(Long configurationId) {
        String userId = this.getId();
        return configurationRepository.findById(configurationId)
                .filter(c1 -> c1.getTeam() != null)
                .filter(c2 -> c2.getTeam().getCourse() != null)
                .map(c -> c.getTeam().getCourse().getTeachers()
                        .stream()
                        .anyMatch(t -> t.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }

    /**
     *
     * @param vmId
     * @return true if the authenticated student owns vmId
     */
    @Override
    public boolean isOwnerOf(Long vmId) {
        String userId = this.getId();
        return virtualMachineRepository.findById(vmId)
                .map(vm -> vm.getOwners()
                        .stream()
                        .anyMatch(o -> o.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }

    /**
     *
     * @param vmId
     * @return true if the authenticated student is part of the team from which vmId has been created
     */
    @Override
    public boolean canUse(Long vmId) {
        String userId = this.getId();
        return virtualMachineRepository.findById(vmId)
                .filter(vm -> vm.getTeam() != null)
                .map(vm -> vm.getTeam().getMembers()
                        .stream()
                        .anyMatch(s -> s.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }

    /**
     *
     * @param vmId
     * @return true if the course of the team that has created vmId is taught by the authenticated teacher
     */
    @Override
    public boolean canConnect(Long vmId) {
        String userId = this.getId();
        return virtualMachineRepository.findById(vmId)
                .filter(vm1 -> vm1.getTeam() != null)
                .filter(vm2 -> vm2.getTeam().getCourse() != null)
                .map(virtualMachine -> virtualMachine.getTeam().getCourse().getTeachers()
                        .stream()
                        .anyMatch(t -> t.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }

    /**
     *
     * @param teamId
     * @return true if the course of teamId is taught by the authenticated teacher
     */
    @Override
    public boolean isHelping(Long teamId) {
        String userId = this.getId();
        return teamRepository.findById(teamId)
                .filter(t -> t.getCourse() != null)
                .map(t -> t.getCourse().getTeachers()
                        .stream()
                        .anyMatch(teacher -> teacher.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }

    /**
     *
     * @param modelId
     * @return true if the course of modelId is taught by the authenticated teacher
     */
    @Override
    public boolean hasDefined(Long modelId) {
        String userId = this.getId();
        return virtualMachineModelRepository.findById(modelId)
                .filter(m1 -> m1.getCourse() != null)
                .map(m -> m.getCourse().getTeachers()
                        .stream()
                        .anyMatch(t -> t.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }

    /**
     *
     * @param modelId
     * @return true if the authenticated user is enrolled in the course of modelId
     */
    @Override
    public boolean canAccess(Long modelId) {
        String userId = this.getId();
        return virtualMachineModelRepository.findById(modelId)
                .filter(m1 -> m1.getCourse() != null)
                .map(m -> m.getCourse().getStudents()
                        .stream()
                        .anyMatch(s -> s.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }

    /**
     *
     * @return true if the authenticated user is enrolled to the course of the exerciseId
     */
    @Override
    public boolean canView(Long exerciseId) {
        String userId = this.getId();
        return exerciseRepository.findById(exerciseId)
                .map(exercise -> exercise.getCourse().getStudents()
                    .stream()
                    .anyMatch(s -> s.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }
    /**
     *
     * @return true if the course of the exerciseId is taught by the authenticated teacher
     */
    @Override
    public boolean canOpen(Long exerciseId) {
        String userId = this.getId();
        return exerciseRepository.findById(exerciseId)
                .map(exercise -> exercise.getCourse().getTeachers()
                        .stream()
                        .anyMatch(t -> t.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }

    /**
     *
     * @return true if the assignments of the exercise is done by the authenticated user
     */
    public boolean isDone(Long exerciseId) {
        String userId = this.getId();
        return exerciseRepository.findById(exerciseId).map(
                e -> e.getPapers()
        .stream().anyMatch(assignment -> assignment.getStudent().getId().equalsIgnoreCase(userId)))
        .orElse(false);
    }

    @Override
    /**
     *
     * @return true if the assignment is done by the authenticated user
     */
    public boolean isAuthor(Long assignmentId) {
        String userId = this.getId();
        return paperRepository.findById(assignmentId)
                .map(a -> a.getStudent().getId().equalsIgnoreCase(userId))
                .orElse(false);
    }

    @Override
    /**
     *
     * @return true if the assignments can be review by the teacher
     */
    public boolean isReview(Long assignmentId) {
        String userId = this.getId();
        return paperRepository.findById(assignmentId)
                .map(a -> a.getAssignment().getCourse().getTeachers()
                .stream().anyMatch(teacher -> teacher.getId().equalsIgnoreCase(userId)))
                .orElse(false);
    }

    @Override
    /**
     *
     * @return true if the authenticated user owns the token
     */
    public boolean hasToken(String tokenId) {
        String userId = this.getId();
        return tokenRepository.findById(tokenId)
                .map(t -> t.getStudentId().equalsIgnoreCase(userId)).orElse(false);
    }

    /**
     *
     * @param configurationId
     * @return true if the authenticated the student is part of the team given configurationId
     */
    @Override
    public boolean canSee(Long configurationId) {
        String userId = this.getId();
        return configurationRepository.findById(configurationId)
                .filter(c -> c.getTeam() != null)
                .filter(c -> c.getTeam().getMembers().size() > 0)
                .map(c -> c.getTeam().getMembers()
                        .stream().map(Student::getId).anyMatch(id -> id.equalsIgnoreCase(userId)))
                .orElse(false);
    }

    /**
     *
     * @return the id of the authenticated user
     */
    @Override
    public String getId() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            String username = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
            return username;
        }
        throw new PrincipalNotFoundException();
    }
}
