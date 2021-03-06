package it.polito.ai.backend.controllers;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import io.swagger.v3.oas.annotations.Operation;
import it.polito.ai.backend.dtos.*;
import it.polito.ai.backend.entities.TeamStatus;
import it.polito.ai.backend.services.Utils;
import it.polito.ai.backend.services.assignment.AssignmentService;
import it.polito.ai.backend.services.notification.NotificationService;
import it.polito.ai.backend.services.team.*;
import it.polito.ai.backend.services.vm.VirtualMachineService;
import net.minidev.json.JSONObject;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/API/courses")
@Validated
public class CourseController {

    @Autowired
    TeamService teamService;
    @Autowired
    NotificationService notificationService;
    @Autowired
    AssignmentService assignmentService;
    @Autowired
    VirtualMachineService virtualMachineService;
    @Autowired
    ModelMapper modelMapper;


    @Operation(summary = "get course")
    @GetMapping("/{courseId}")
    ResponseEntity<CourseDTO> getOne(@PathVariable @NotBlank String courseId) {
        CourseDTO courseDTO = teamService.getCourse(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));
        Long modelId = virtualMachineService.getVirtualMachineModelForCourse(courseDTO.getId()).map(VirtualMachineModelDTO::getId).orElse(null);
        return new ResponseEntity<>(ModelHelper.enrich(courseDTO, modelId), HttpStatus.OK) ;
    }

    @Operation(summary = "get enrolled students")
    @GetMapping("/{courseId}/enrolled")
    ResponseEntity<CollectionModel<JSONObject>> enrolledStudents(@PathVariable @NotBlank String courseId) {
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CourseController.class).enrolledStudents(courseId)).withSelfRel();
        List<StudentDTO> enrolledStudents = teamService.getEnrolledStudents(courseId).stream().map(ModelHelper::enrich).collect(Collectors.toList());
        List<JSONObject> list = new ArrayList<>();
        Map<StudentDTO,String> memberAndStatus = new HashMap<>();

        for (StudentDTO s:enrolledStudents) {
            Optional<TeamDTO> teamDTO = teamService.getTeamForStudentAndCourse(s.getId(),courseId);
            if(teamDTO.isPresent() && teamDTO.get().getStatus()== TeamStatus.ACTIVE){
                String teamName = teamService.getTeamForStudentAndCourse(s.getId(),courseId).get().getName();
                memberAndStatus.put(s,teamName);
            }
            else memberAndStatus.put(s,"");
        }

        memberAndStatus.forEach( (k, v) ->{
            JSONObject entity = new JSONObject();
            entity.put("student" , k);
            entity.put("teamName",v);
            list.add(entity);
        });


        return new ResponseEntity<>(
                CollectionModel.of(list, selfLink),
                HttpStatus.OK);

    }

    @Operation(summary = "get teams")
    @GetMapping("/{courseId}/teams")
    ResponseEntity<CollectionModel<TeamDTO>> getTeams(@PathVariable @NotBlank String courseId) {
        List<TeamDTO> teams = teamService.getTeamsForCourse(courseId)
                .stream()
                .map(t -> {
                    Long configurationId = virtualMachineService.getConfigurationForTeam(t.getId()).map(ConfigurationDTO::getId).orElse(null);
                    return ModelHelper.enrich(t, courseId, configurationId);
                })
                .collect(Collectors.toList());
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CourseController.class).getTeams(courseId)).withSelfRel();
        return new ResponseEntity<>(CollectionModel.of(teams, selfLink), HttpStatus.OK);
    }

    @Operation(summary = "get teachers")
    @GetMapping("/{courseId}/teachers")
    ResponseEntity<CollectionModel<TeacherDTO>> getTeachers(@PathVariable @NotBlank String courseId) {
        List<TeacherDTO> teachers = teamService.getTeachersForCourse(courseId).stream().map(ModelHelper::enrich).collect(Collectors.toList());
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CourseController.class).getTeachers(courseId)).withSelfRel();
        return new ResponseEntity<>(CollectionModel.of(teachers, selfLink), HttpStatus.OK);
    }

    @Operation(summary = "delete course")
    @DeleteMapping({"/{courseId}"})
    void deleteCourse(@PathVariable @NotBlank String courseId){
        if (!teamService.deleteCourse(courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "course is enabled or is associated with one or more entities");
        }
    }

    @Operation(summary = "update course")
    @PutMapping("/{courseId}")
    ResponseEntity<CourseDTO> updateCourse(@RequestBody @Valid CourseDTO courseDTO, @PathVariable @NotBlank String courseId){
        CourseDTO courseDTO1 = teamService.updateCourse(courseId, courseDTO);
        Long modelId = virtualMachineService.getVirtualMachineModelForCourse(courseId).map(VirtualMachineModelDTO::getId).orElse(null);
        return new ResponseEntity<>(ModelHelper.enrich(courseDTO1, modelId), HttpStatus.OK);

    }

    @Operation(summary = "create a new course")
    @PostMapping({"", "/"})
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<CourseDTO> addCourse(@RequestBody @Valid CourseDTO courseDTO) {
        if (!teamService.addCourse(courseDTO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format("course %s already exists", courseDTO.getId()));
        }
        return new ResponseEntity<>(ModelHelper.enrich(courseDTO, null), HttpStatus.CREATED);
    }

    @Operation(summary = "get all students enrolled to the course that are part of an active team")
    @GetMapping("/{courseId}/teams/students")
    ResponseEntity<CollectionModel<StudentDTO>> getStudentsInTeams(@PathVariable @NotBlank String courseId) {
        List<StudentDTO> studentsInTeams = teamService.getStudentsInTeams(courseId).stream().map(ModelHelper::enrich).collect(Collectors.toList());
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CourseController.class).getStudentsInTeams(courseId)).withSelfRel();
        return new ResponseEntity<>(CollectionModel.of(studentsInTeams, selfLink), HttpStatus.OK);
    }

    @Operation(summary = "get all students enrolled to the course not being part of an active team")
    @GetMapping("/{courseId}/teams/available-students")
    ResponseEntity<CollectionModel<StudentDTO>> getAvailableStudents(@PathVariable @NotBlank String courseId) {
        List<StudentDTO> availableStudents = teamService.getAvailableStudents(courseId).stream().map(ModelHelper::enrich).collect(Collectors.toList());
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CourseController.class).getAvailableStudents(courseId)).withSelfRel();
        return new ResponseEntity<>(CollectionModel.of(availableStudents, selfLink), HttpStatus.OK);
    }

    @Operation(summary = "enable a course")
    @PostMapping("/{courseId}/enable")
    void enable(@PathVariable @NotBlank String courseId) {
        teamService.enableCourse(courseId);
    }

    @Operation(summary = "disable a course")
    @PostMapping("/{courseId}/disable")
    void disable(@PathVariable @NotBlank String courseId) {
        teamService.disableCourse(courseId);
    }

    @Operation(summary = "enroll an existing student to a course")
    @PostMapping("/{courseId}/enrollOne")
    void addStudent(@RequestBody @Valid EnrollmentRequest request, @PathVariable @NotBlank String courseId) {

        String studentId = request.getStudentId();

        if (!teamService.addStudentToCourse(studentId, courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format("student %s already inserted", studentId));
        }
        assignmentService.getAssignmentsForCourse(courseId).forEach(
                assignmentDTO -> {
                    if(assignmentDTO.getExpired().after(Utils.getNow()))
                    assignmentService.setPapersNullForAssignment(assignmentDTO.getId());
                }
        );


    }

    @Operation(summary = "remove an enrolled students from a course")
    @DeleteMapping("/{courseId}/enrolled/{studentId}")
    void removeStudent(@PathVariable @NotBlank String courseId, @PathVariable @NotBlank String studentId) {
        if (!teamService.removeStudentFromCourse(studentId, courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "the student is part of a team");
        }
    }

    @Operation(summary = "add an existing teacher to the course management")
    @PostMapping("/{courseId}/teachers")
    void addTeacher(@RequestBody @Valid ManagementRequest request, @PathVariable @NotBlank String courseId) {
        String teacherId = request.getTeacherId();

        if (!teamService.addTeacherToCourse(teacherId, courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format("teacher %s already inserted", teacherId));
        }
    }

    /*List<String> mimeTypes = Arrays.asList(
            "text/plain",
            "text/x-csv",
            "application/vnd.ms-excel",
            "application/csv",
            "application/x-csv",
            "text/csv",
            "text/comma-separated-values",
            "text/x-comma-separated-values",
            "text/tab-separated-values");*/

    @Operation(summary = "add new students and enroll them to a course by uploading a csv file")
    @PostMapping("/{courseId}/enrollMany")
    List<Boolean> enrollStudents(@RequestParam("file") MultipartFile file, @PathVariable @NotBlank String courseId) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file empty");
        }

        try {
            TikaConfig tika = new TikaConfig();
            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, file.getOriginalFilename());
            MediaType mimeType = tika.getDetector().detect(TikaInputStream.get(file.getBytes()), metadata);
            String type = mimeType.toString();
            if (!type.equalsIgnoreCase("text/csv")) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, type);
            }

            List<Boolean> addedAndEnrolledStudents = new ArrayList<>();

            if (!file.isEmpty()) {
                Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                addedAndEnrolledStudents = teamService.addAndEnroll(reader, courseId);
                assignmentService.getAssignmentsForCourse(courseId).forEach(
                        assignmentDTO -> {
                            if(assignmentDTO.getExpired().after(Utils.getNow()))
                                assignmentService.setPapersNullForAssignment(assignmentDTO.getId());
                        }
                );

            }

            return addedAndEnrolledStudents;
        } catch (TikaException | IOException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "invalid file content");
        }


    }

    @Operation(summary = "enroll existing students to a course by uploading a csv file")
    @PostMapping("/{courseId}/enrollAll")
    List<Boolean> enrollAll(@RequestParam("file") MultipartFile file, @PathVariable @NotBlank String courseId) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file empty");
        }

        try {
            TikaConfig tika = new TikaConfig();
            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, file.getOriginalFilename());
            MediaType mimeType = tika.getDetector().detect(TikaInputStream.get(file.getBytes()), metadata);
            String type = mimeType.toString();
            if (!type.equalsIgnoreCase("text/csv")) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, type);
            }

            List<Boolean> enrolledStudents = new ArrayList<>();

            if (!file.isEmpty()) {
                Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                CsvToBean<StudentDTO> csvToBean = new CsvToBeanBuilder(reader)
                        .withType(StudentDTO.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .build();
                List<StudentDTO> students = csvToBean.parse();
                List<String> studentIds = students.stream().map(StudentDTO::getId).collect(Collectors.toList());
                enrolledStudents = teamService.enrollAll(studentIds, courseId);
                assignmentService.getAssignmentsForCourse(courseId).forEach(
                        assignmentDTO -> {
                            if(assignmentDTO.getExpired().after(Utils.getNow()))
                                assignmentService.setPapersNullForAssignment(assignmentDTO.getId());
                        }
                );
            }

            return enrolledStudents;
        } catch (TikaException | IOException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "invalid file content");
        }


    }


    @Operation(summary = "create a new unconfirmed team in a course")
    @PostMapping("/{courseId}/createTeam")
    @ResponseStatus(HttpStatus.CREATED)
       ResponseEntity<TeamDTO> createTeam(@RequestBody @Valid TeamCreationRequest teamCreationRequest, @PathVariable String courseId) {
        try {

                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                Timestamp timeout = new Timestamp(format.parse(teamCreationRequest.getTimeout()).getTime());
                if(timeout.before(Utils.getNow()))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid timeout");
                String teamName = teamCreationRequest.getTeamName();


                Optional<StudentDTO> proponent = teamService.getStudent(teamCreationRequest.getStudentId());
                if(!proponent.isPresent())
                    throw new StudentNotFoundException(teamCreationRequest.getStudentId());

                List<String> memberIds = teamCreationRequest.getMemberIds();
                //add the student proposing team for control
                memberIds.add(proponent.get().getId());

                TeamDTO team = teamService.proposeTeam(courseId, teamName, memberIds);

                //remove the student proposing team because no where to confirm
                memberIds.remove(proponent.get().getId());
                notificationService.notifyTeam(team, memberIds,timeout,proponent.get().getId());

                return new ResponseEntity<>(ModelHelper.enrich(team, courseId, null), HttpStatus.CREATED);


            } catch (ParseException  e) {
                e.printStackTrace();
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "invalid file content");
            }

    }

    @Operation(summary = "create a new assignment for a course")
    @PostMapping("/{courseId}/assignment")
    void createAssignment(@RequestPart("image") MultipartFile file, @RequestPart("expiredDate") String expiredDate, @PathVariable @NotBlank String courseId){
        try {
            Utils.checkTypeImage(file);
            System.out.println("Original Image Byte Size - " + file.getBytes().length);
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            Timestamp expired = new Timestamp(format.parse(expiredDate).getTime());
            Timestamp published = Utils.getNow();
            AssignmentDTO a = assignmentService.addAssignmentForCourse(courseId,published,expired,file);
            assignmentService.setPapersNullForAssignment(a.getId());
        } catch (ParseException | IOException | TikaException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "invalid file content");
        }
    }

    @Operation(summary = "get all assignments of a course")
    @GetMapping("/{courseId}/assignments")
    ResponseEntity<CollectionModel<AssignmentDTO>> getAssignments(@PathVariable @NotBlank String courseId){
        List<AssignmentDTO> assignmentDTOS = assignmentService.getAssignmentsForCourse(courseId).stream()
                .map(e -> ModelHelper.enrich(e,courseId)).collect(Collectors.toList());
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CourseController.class).getAssignments(courseId)).withSelfRel();
        return new ResponseEntity<>(CollectionModel.of(assignmentDTOS, selfLink), HttpStatus.OK);


    }

}
