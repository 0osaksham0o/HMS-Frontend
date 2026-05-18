package com.hospital.frontend.controller;

import com.hospital.frontend.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller @RequestMapping("/departments")
public class DepartmentWebController {
    @Autowired private ApiService api;
    private static final String API      = "/api/departments";
    private static final String PHY_API  = "/api/physicians";

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page, Model m) {
        m.addAttribute("page", api.getPage(API, null, page)); return "departments/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable int id, Model m) {
        Map<String, Object> dept = api.getOne(API + "/" + id);
        m.addAttribute("item", dept);

        List<Map<String, Object>> allPhysicians   = api.getList(PHY_API);
        List<Map<String, Object>> allAffiliations = api.getList("/api/affiliations");

        Map<String, Map<String, Object>> phyById = new HashMap<>();
        for (Map<String, Object> phy : allPhysicians) {
            Object eid = phy.get("employeeId");
            if (eid != null) phyById.put(String.valueOf(eid), phy);
        }

        List<Map<String, Object>> affiliatedPhysicians = new java.util.ArrayList<>();
        for (Map<String, Object> aff : allAffiliations) {
            Object deptId = aff.get("departmentId");
            Object phyId  = aff.get("physicianId");
            Object idObj  = aff.get("id");
            if (idObj instanceof Map) {
                Map<?, ?> idMap = (Map<?, ?>) idObj;
                if (deptId == null) deptId = idMap.get("departmentId");
                if (phyId  == null) phyId  = idMap.get("physicianId");
            }
            if (deptId != null && String.valueOf(deptId).equals(String.valueOf(id))) {
                Map<String, Object> phy = phyId != null ? phyById.get(String.valueOf(phyId)) : null;
                Map<String, Object> enriched = new LinkedHashMap<>(aff);
                enriched.put("physicianName",       phy != null ? phy.get("name")       : "Physician #" + phyId);
                enriched.put("physicianEmployeeId", phyId);
                affiliatedPhysicians.add(enriched);
            }
        }
        m.addAttribute("affiliatedPhysicians", affiliatedPhysicians);

        return "departments/view";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        if (!m.containsAttribute("item")) m.addAttribute("item", new HashMap<>());
        m.addAttribute("isNew", true);
        m.addAttribute("physicians", api.getList(PHY_API));
        return "departments/form";
    }

    @PostMapping
    public String create(@RequestParam Map<String,String> p, Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("departmentId", Integer.parseInt(p.get("departmentId")));
            d.put("name", p.get("name"));
            d.put("headId", Integer.parseInt(p.get("headId")));
            api.post(API, d);
            ra.addFlashAttribute("success", "Department created successfully.");
            return "redirect:/departments";
        } catch (Exception e) {
            m.addAttribute("error", e.getMessage());
            m.addAttribute("item", p);
            m.addAttribute("isNew", true);
            m.addAttribute("physicians", api.getList(PHY_API));
            return "departments/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id, Model m) {
        m.addAttribute("item", api.getOne(API+"/"+id));
        m.addAttribute("isNew", false);
        m.addAttribute("physicians", api.getList(PHY_API));
        return "departments/form";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable int id, @RequestParam Map<String,String> p,
                         Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("departmentId", id);
            d.put("name", p.get("name"));
            d.put("headId", Integer.parseInt(p.get("headId")));
            api.put(API+"/"+id, d);
            ra.addFlashAttribute("success", "Department updated successfully.");
            return "redirect:/departments/"+id;
        } catch (Exception e) {
            Map<String,Object> item = new HashMap<>(p);
            item.put("departmentId", id);
            m.addAttribute("error", e.getMessage());
            m.addAttribute("item", item);
            m.addAttribute("isNew", false);
            m.addAttribute("physicians", api.getList(PHY_API));
            return "departments/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id, RedirectAttributes ra) {
        try { api.delete(API+"/"+id); ra.addFlashAttribute("success","Department deleted."); }
        catch(Exception e){ ra.addFlashAttribute("error","Cannot delete: "+e.getMessage()); }
        return "redirect:/departments";
    }
}
