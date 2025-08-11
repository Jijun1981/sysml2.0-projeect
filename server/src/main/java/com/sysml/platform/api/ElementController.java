package com.sysml.platform.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sysml.platform.infrastructure.emf.ElementRepository;

import java.util.*;

/**
 * 元素管理控制器
 */
@RestController
@RequestMapping("/api")
public class ElementController {
    
    @Autowired
    private ElementRepository elementRepository;
    
    @PostMapping("/elements")
    public ResponseEntity<Map<String, Object>> createElement(@RequestBody Map<String, Object> element) {
        String id = UUID.randomUUID().toString();
        element.put("id", id);
        element.put("created", new Date());
        
        elementRepository.save(element);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(element);
    }
    
    @GetMapping("/elements/{id}")
    public ResponseEntity<Map<String, Object>> getElement(@PathVariable String id) {
        Map<String, Object> element = elementRepository.findById(id);
        if (element == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(element);
    }
    
    @PostMapping("/relations")
    public ResponseEntity<Map<String, Object>> createRelation(@RequestBody Map<String, Object> relation) {
        String id = UUID.randomUUID().toString();
        relation.put("id", id);
        
        elementRepository.saveRelation(relation);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(relation);
    }
}