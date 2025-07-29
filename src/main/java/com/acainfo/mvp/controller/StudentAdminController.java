package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.common.PageResponseDto;
import com.acainfo.mvp.dto.student.CreateStudentDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.mapper.StudentMapper;
import com.acainfo.mvp.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/students")
@SecurityRequirement(name = "sessionAuth")
@Tag(name = "Student Administration", description = "Endpoints para la gestión administrativa de estudiantes")
public class StudentAdminController {

    private final StudentService studentService;
    private final StudentMapper studentMapper;  // Añadido mapper como atributo

    @Operation(summary = "Crear un nuevo estudiante")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estudiante creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PostMapping
    public ResponseEntity<StudentDto> createStudent(@RequestBody @Valid CreateStudentDto createStudentDto) {
        log.info("Creando nuevo estudiante: {}", createStudentDto);
        StudentDto studentDto = studentService.createStudent(createStudentDto);
        return new ResponseEntity<>(studentDto, HttpStatus.CREATED);
    }

    @Operation(summary = "Listar estudiantes paginados")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<PageResponseDto<StudentDto>> listStudents(@PageableDefault Pageable pageable) {
        log.info("Listando estudiantes, página: {}", pageable);
        var studentPage = studentService.getAllStudents(pageable);
        var response = studentMapper.toPageResponseDto(studentPage);  // Usando el mapper
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar estudiante por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudiante eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Estudiante no encontrado o tiene inscripciones activas")
    })
    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long studentId) {
        log.info("Eliminando estudiante con ID: {}", studentId);
        studentService.deleteStudent(studentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Actualizar información del estudiante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudiante actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    })
    @PutMapping("/{studentId}")
    public ResponseEntity<StudentDto> updateStudent(
            @PathVariable Long studentId,
            @RequestBody @Valid StudentDto updateDto) {
        log.info("Actualizando estudiante con ID: {}", studentId);
        StudentDto updatedStudent = studentService.updateStudent(studentId, updateDto);
        return ResponseEntity.ok(updatedStudent);
    }

    @Operation(summary = "Obtener estadísticas generales de estudiantes")
    @GetMapping("/stats")
    public ResponseEntity<?> getAdminStudentStats() {
        log.info("Obteniendo estadísticas administrativas de estudiantes");
        var stats = studentService.getAdminStudentStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Verificar existencia de email")
    @GetMapping("/email-exists")
    public ResponseEntity<Boolean> emailExists(@RequestParam String email) {
        log.info("Verificando existencia del email: {}", email);
        boolean exists = studentService.emailExists(email);
        return ResponseEntity.ok(exists);
    }
}
