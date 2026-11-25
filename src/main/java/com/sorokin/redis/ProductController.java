//package com.sorokin.redis;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/products")
//@RequiredArgsConstructor
//public class ProductController {
//
//    private final ProductService service;
//
//    @PostMapping
//    public Product create(@RequestBody Product product) {
//        return service.create(product);
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<Product> getById(
//            @PathVariable Long id,
//            @RequestParam(defaultValue = "true") boolean useCache) {
//        return service.getById(id, useCache)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product updated) {
//        return service.update(id, updated)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> delete(@PathVariable Long id) {
//        return service.delete(id)
//                ? ResponseEntity.noContent().build()
//                : ResponseEntity.notFound().build();
//    }
//}
package com.sorokin.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody Product product) {
        return service.create(product);
    }


    @PostMapping("/{id}/purchase")
    public String purchase(@PathVariable Long id) {
        return service.purchase(id);
    }


    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product updated) {
        return service.update(id, updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
