package com.acainfo.mvp.security;

import com.acainfo.mvp.model.Admin;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.Teacher;
import com.acainfo.mvp.repository.AdminRepository;
import com.acainfo.mvp.repository.StudentRepository;
import com.acainfo.mvp.repository.TeacherRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Servicio personalizado para cargar usuarios durante la autenticación.
 * Busca tanto en estudiantes como en profesores.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AdminRepository adminRepository;

    public CustomUserDetailsService(StudentRepository studentRepository,
                                    TeacherRepository teacherRepository,
                                    AdminRepository adminRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Primero buscar en estudiantes
        Optional<Student> student = studentRepository.findByEmail(email);
        if (student.isPresent()) {
            return createUserDetails(student.get());
        }

        // Si no es estudiante, buscar en profesores
        Optional<Teacher> teacher = teacherRepository.findByEmail(email);
        if (teacher.isPresent()) {
            return createUserDetails(teacher.get());
        }

        Optional<Admin> admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) {
            return createUserDetails(admin.get());
        }

        throw new UsernameNotFoundException("Usuario no encontrado con email: " + email);
    }

    private CustomUserDetails createUserDetails(Student student) {
        return new CustomUserDetails(
                student.getId(),
                student.getEmail(),
                student.getPassword(),
                student.getName(),
                "STUDENT"
        );
    }

    private CustomUserDetails createUserDetails(Teacher teacher) {
        return new CustomUserDetails(
                teacher.getId(),
                teacher.getEmail(),
                teacher.getPassword(),
                teacher.getName(),
                "TEACHER"
        );
    }

    private CustomUserDetails createUserDetails(Admin admin) {
        return new CustomUserDetails(
                admin.getId(),
                admin.getEmail(),
                admin.getPassword(),
                admin.getName(),
                "ADMIN"
        );
    }
}
