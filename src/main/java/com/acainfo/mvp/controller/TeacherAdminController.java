package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.common.PageResponseDto;
import com.acainfo.mvp.dto.teacher.CreateTeacherDto;
import com.acainfo.mvp.dto.teacher.TeacherDto;
import com.acainfo.mvp.mapper.TeacherMapper;
import com.acainfo.mvp.service.TeacherService;
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

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/teachers")
@SecurityRequirement(name = "sessionAuth")
@Tag(name = "Teacher Administration", description = "Endpoints para la gestión administrativa de profesores")
public class TeacherAdminController {

    private final TeacherService teacherService;
    private final TeacherMapper teacherMapper;

    @Operation(summary = "Crear un nuevo profesor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Profesor creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PostMapping
    public ResponseEntity<TeacherDto> createTeacher(@RequestBody @Valid CreateTeacherDto createTeacherDto) {
        log.info("Creando nuevo profesor: {}", createTeacherDto);
        TeacherDto teacherDto = teacherService.createTeacher(createTeacherDto);
        return new ResponseEntity<>(teacherDto, HttpStatus.CREATED);
    }

    @Operation(summary = "Listar profesores paginados")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<TeacherDto>> listTeachers() {
        log.info("Listando profesores");
        var response = teacherService.getAllTeachers();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar profesor por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profesor eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Profesor no encontrado o con grupos activos asignados")
    })
    @DeleteMapping("/{teacherId}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long teacherId) {
        log.info("Eliminando profesor con ID: {}", teacherId);
        teacherService.deleteTeacher(teacherId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Actualizar información del profesor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profesor actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Profesor no encontrado")
    })
    @PutMapping("/{teacherId}")
    public ResponseEntity<TeacherDto> updateTeacher(
            @PathVariable Long teacherId,
            @RequestBody @Valid TeacherDto updateDto) {
        log.info("Actualizando profesor con ID: {}", teacherId);
        TeacherDto updatedTeacher = teacherService.updateTeacher(teacherId, updateDto);
        return ResponseEntity.ok(updatedTeacher);
    }

    @Operation(summary = "Obtener estadísticas generales de profesores")
    @GetMapping("/stats")
    public ResponseEntity<?> getAdminTeacherStats() {
        log.info("Obteniendo estadísticas administrativas de profesores");
        var stats = teacherService.getAdminTeacherStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Verificar existencia de email")
    @GetMapping("/email-exists")
    public ResponseEntity<Boolean> emailExists(@RequestParam String email) {
        log.info("Verificando existencia del email: {}", email);
        boolean exists = teacherService.emailExists(email);
        return ResponseEntity.ok(exists);
    }
}
