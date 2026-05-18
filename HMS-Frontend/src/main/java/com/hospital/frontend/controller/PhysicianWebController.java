package com.hospital.frontend.controller;

import com.hospital.frontend.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller @RequestMapping("/physicians")
public class PhysicianWebController {
    @Autowired private ApiService api;
    private static final String API = "/api/physicians";

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="") String search, Model m) {
        m.addAttribute("page", search.isBlank()
                ? api.getPage(API, null, page)
                : api.getPageSearch(API, search, page));
        m.addAttribute("search", search);
        return "physicians/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable int id, Model m) {
        Map<String, Object> physician = api.getOne(API + "/" + id);
        m.addAttribute("item", physician);

        // ── Procedures (Trained_In) ────────────────────────────────────────
        List<Map<String, Object>> allProcedures = api.getList("/api/procedures");
        List<Map<String, Object>> allTrainedIn  = api.getList("/api/trainedin");

        // Build a lookup map: procedureCode → procedure map
        Map<String, Map<String, Object>> procByCode = new HashMap<>();
        for (Map<String, Object> proc : allProcedures) {
            Object code = proc.get("code");
            if (code != null) procByCode.put(String.valueOf(code), proc);
        }

        // Filter TrainedIn rows for this physician and enrich with procedure info
        // Spring Data REST (HATEOAS) nests IDs inside an "id" sub-object: { "id": {"physicianId":3, "treatmentCode":1} }
        List<Map<String, Object>> certifiedProcedures = new ArrayList<>();
        StringBuilder procNamesBuilder = new StringBuilder();
        for (Map<String, Object> ti : allTrainedIn) {
            // Support both flat (DTO) and nested HATEOAS id format
            Object phyId = ti.get("physicianId");
            Object treatCode = ti.get("treatmentCode");
            Object idObj = ti.get("id");
            if (idObj instanceof Map) {
                Map<?, ?> idMap = (Map<?, ?>) idObj;
                if (phyId == null) phyId = idMap.get("physicianId");
                if (treatCode == null) treatCode = idMap.get("treatmentCode");
            }
            if (phyId != null && String.valueOf(phyId).equals(String.valueOf(id))) {
                Map<String, Object> proc = treatCode != null ? procByCode.get(String.valueOf(treatCode)) : null;
                Map<String, Object> enriched = new LinkedHashMap<>(ti);
                enriched.put("procedureName", proc != null ? proc.get("name") : "Procedure #" + treatCode);
                Object cost = proc != null ? proc.get("cost") : null;
                enriched.put("procedureCost", cost != null ? "$" + cost.toString().replaceAll("\\.0$", "") : "—");
                enriched.put("procedureCode", treatCode);
                certifiedProcedures.add(enriched);
                if (procNamesBuilder.length() > 0) procNamesBuilder.append(", ");
                procNamesBuilder.append(proc != null ? proc.get("name") : "Procedure #" + treatCode);
            }
        }
        m.addAttribute("certifiedProcedures", certifiedProcedures);
        m.addAttribute("procedureNames", procNamesBuilder.length() > 0 ? procNamesBuilder.toString() : "—");

        // ── Department Affiliations ────────────────────────────────────────
        List<Map<String, Object>> allDepts = api.getList("/api/departments");
        List<Map<String, Object>> allAffiliations = api.getList("/api/affiliations");

        // Build a lookup map: departmentId → department map
        Map<String, Map<String, Object>> deptById = new HashMap<>();
        for (Map<String, Object> dept : allDepts) {
            Object deptId = dept.get("departmentId");
            if (deptId != null) deptById.put(String.valueOf(deptId), dept);
        }

        // Filter affiliations for this physician and enrich with department info
        // Spring Data REST (HATEOAS) nests IDs inside an "id" sub-object: { "id": {"physicianId":3, "departmentId":1} }
        List<Map<String, Object>> affiliations = new ArrayList<>();
        for (Map<String, Object> aff : allAffiliations) {
            Object phyId = aff.get("physicianId");
            Object deptId = aff.get("departmentId");
            Object idObj = aff.get("id");
            if (idObj instanceof Map) {
                Map<?, ?> idMap = (Map<?, ?>) idObj;
                if (phyId == null) phyId = idMap.get("physicianId");
                if (deptId == null) deptId = idMap.get("departmentId");
            }
            if (phyId != null && String.valueOf(phyId).equals(String.valueOf(id))) {
                Map<String, Object> dept = deptId != null ? deptById.get(String.valueOf(deptId)) : null;
                Map<String, Object> enriched = new LinkedHashMap<>(aff);
                enriched.put("departmentName", dept != null ? dept.get("name") : "Department #" + deptId);
                enriched.put("departmentIdResolved", deptId);
                affiliations.add(enriched);
            }
        }
        m.addAttribute("affiliations", affiliations);

        return "physicians/view";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        if (!m.containsAttribute("item")) m.addAttribute("item", new HashMap<>());
        m.addAttribute("isNew", true);
        return "physicians/form";
    }

    @PostMapping
    public String create(@RequestParam Map<String,String> p, Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("employeeId", Integer.parseInt(p.get("employeeId")));
            d.put("name", p.get("name"));
            d.put("position", p.get("position"));
            d.put("ssn", Integer.parseInt(p.get("ssn")));
            api.post(API, d);
            ra.addFlashAttribute("success", "Physician created successfully.");
            return "redirect:/physicians";
        } catch (Exception e) {
            // Stay on form — show error, preserve entered values
            m.addAttribute("error", e.getMessage());
            m.addAttribute("item", p);
            m.addAttribute("isNew", true);
            return "physicians/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id, Model m) {
        m.addAttribute("item", api.getOne(API+"/"+id)); m.addAttribute("isNew",false); return "physicians/form";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable int id, @RequestParam Map<String,String> p,
                         Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("employeeId", id); d.put("name", p.get("name"));
            d.put("position", p.get("position"));
            d.put("ssn", Integer.parseInt(p.get("ssn")));
            api.put(API+"/"+id, d);
            ra.addFlashAttribute("success", "Physician updated successfully.");
            return "redirect:/physicians/"+id;
        } catch (Exception e) {
            Map<String,Object> item = new HashMap<>(p);
            item.put("employeeId", id);
            m.addAttribute("error", e.getMessage());
            m.addAttribute("item", item);
            m.addAttribute("isNew", false);
            return "physicians/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id, RedirectAttributes ra) {
        try { api.delete(API+"/"+id); ra.addFlashAttribute("success","Physician deleted."); }
        catch(Exception e){ ra.addFlashAttribute("error","Cannot delete: "+e.getMessage()); }
        return "redirect:/physicians";
    }
}
