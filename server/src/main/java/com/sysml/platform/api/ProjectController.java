package com.sysml.platform.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sysml.platform.infrastructure.emf.ProjectRepository;

import java.util.*;

/**
 * 项目管理控制器
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(@RequestBody Map<String, Object> project) {
        String id = UUID.randomUUID().toString();
        project.put("id", id);
        project.put("created", new Date());
        
        projectRepository.save(project);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProject(@PathVariable String id) {
        Map<String, Object> project = projectRepository.findById(id);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(project);
    }
}