package com.inventory.catalogservice.service;

import com.inventory.catalogservice.model.Category;
import com.inventory.catalogservice.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        // Get root categories (no parent), then recursively build tree
        List<Category> roots = repo.findByParentIsNull();
        for (Category root : roots) {
            result.add(buildNode(root, 0));
        }
        return result;
    }

    private Map<String, Object> buildNode(Category cat, int depth) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", cat.getId());
        node.put("code", cat.getCode());
        node.put("name", cat.getName());
        node.put("depth", depth);
        node.put("active", cat.getActive());
        node.put("createdAt", cat.getCreatedAt());
        if (cat.getParent() != null) {
            node.put("parentId", cat.getParent().getId());
            node.put("parentName", cat.getParent().getName());
        }
        // Load children
        List<Category> children = repo.findByParentId(cat.getId());
        if (!children.isEmpty()) {
            List<Map<String, Object>> childNodes = new ArrayList<>();
            for (Category child : children) {
                childNodes.add(buildNode(child, depth + 1));
            }
            node.put("children", childNodes);
        }
        return node;
    }

    public Map<String, Object> getById(Long id) {
        Category cat = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        return buildNode(cat, 0);
    }

    @Transactional
    public Category create(Map<String, Object> body) {
        Category cat = new Category();
        applyBody(cat, body);
        return repo.save(cat);
    }

    @Transactional
    public Category update(Long id, Map<String, Object> body) {
        Category cat = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        applyBody(cat, body);
        return repo.save(cat);
    }

    private void applyBody(Category cat, Map<String, Object> body) {
        if (body.containsKey("code")) cat.setCode((String) body.get("code"));
        if (body.containsKey("name")) cat.setName((String) body.get("name"));
        if (body.containsKey("active")) cat.setActive((Boolean) body.get("active"));
        if (body.containsKey("parentId")) {
            Long parentId = body.get("parentId") != null
                    ? ((Number) body.get("parentId")).longValue() : null;
            if (parentId != null) {
                cat.setParent(repo.findById(parentId)
                        .orElseThrow(() -> new RuntimeException("Parent category not found: " + parentId)));
            } else {
                cat.setParent(null);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        Category cat = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        // Reassign children to this category's parent before deleting
        List<Category> children = repo.findByParentId(id);
        for (Category child : children) {
            child.setParent(cat.getParent());
            repo.save(child);
        }
        repo.delete(cat);
    }
}