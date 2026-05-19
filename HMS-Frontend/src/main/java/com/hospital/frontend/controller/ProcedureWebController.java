package com.hospital.frontend.controller;

import com.hospital.frontend.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller @RequestMapping("/procedures")
public class ProcedureWebController {
    @Autowired private ApiService api;
    private static final String API = "/api/procedures";

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page, Model m) {
        m.addAttribute("page", api.getPage(API, null, page)); return "procedures/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable int id, Model m) {
        Map<String, Object> procedure = api.getOne(API + "/" + id);
        m.addAttribute("item", procedure);

        // ── Physicians certified for this procedure ────────────────────────
        List<Map<String, Object>> allPhysicians = api.getList("/api/physicians");
        List<Map<String, Object>> allTrainedIn  = api.getList("/api/trainedin");

        // Build a physician lookup map: employeeId → physician map
        Map<String, Map<String, Object>> phyById = new HashMap<>();
        for (Map<String, Object> phy : allPhysicians) {
            Object eid = phy.get("employeeId");
            if (eid != null) phyById.put(String.valueOf(eid), phy);
        }

        // Filter TrainedIn rows for this procedure code, handling both flat-DTO
        // and HATEOAS nested-id { "id": {"physicianId":…, "treatmentCode":…} }
        List<Map<String, Object>> certifiedPhysicians = new java.util.ArrayList<>();
        for (Map<String, Object> ti : allTrainedIn) {
            Object treatCode = ti.get("treatmentCode");
            Object phyId    = ti.get("physicianId");
            Object idObj    = ti.get("id");
            if (idObj instanceof Map) {
                Map<?, ?> idMap = (Map<?, ?>) idObj;
                if (treatCode == null) treatCode = idMap.get("treatmentCode");
                if (phyId    == null) phyId     = idMap.get("physicianId");
            }
            if (treatCode != null && String.valueOf(treatCode).equals(String.valueOf(id))) {
                Map<String, Object> phy = phyId != null ? phyById.get(String.valueOf(phyId)) : null;
                Map<String, Object> enriched = new LinkedHashMap<>(ti);
                enriched.put("physicianName",       phy != null ? phy.get("name")       : "Physician #" + phyId);
                enriched.put("physicianPosition",   phy != null ? phy.get("position")   : "—");
                enriched.put("physicianEmployeeId", phyId);
                certifiedPhysicians.add(enriched);
            }
        }
        m.addAttribute("certifiedPhysicians", certifiedPhysicians);

        return "procedures/view";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        if (!m.containsAttribute("item")) m.addAttribute("item", new HashMap<>());
        m.addAttribute("isNew", true);
        return "procedures/form";
    }

    @PostMapping
    public String create(@RequestParam Map<String,String> p, Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("code", Integer.parseInt(p.get("code")));
            d.put("name", p.get("name"));
            d.put("cost", Double.parseDouble(p.get("cost")));
            api.post(API, d);
            ra.addFlashAttribute("success", "Procedure created successfully.");
            return "redirect:/procedures";
        } catch (Exception e) {
            m.addAttribute("error", ErrorMessageHelper.friendly(e));
            m.addAttribute("item", p);
            m.addAttribute("isNew", true);
            return "procedures/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id, Model m) {
        m.addAttribute("item", api.getOne(API+"/"+id)); m.addAttribute("isNew",false); return "procedures/form";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable int id, @RequestParam Map<String,String> p,
                         Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("code", id); d.put("name", p.get("name"));
            d.put("cost", Double.parseDouble(p.get("cost")));
            api.put(API+"/"+id, d);
            ra.addFlashAttribute("success", "Procedure updated successfully.");
            return "redirect:/procedures/"+id;
        } catch (Exception e) {
            Map<String,Object> item = new HashMap<>(p);
            item.put("code", id);
            m.addAttribute("error", ErrorMessageHelper.friendly(e));
            m.addAttribute("item", item);
            m.addAttribute("isNew", false);
            return "procedures/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id, RedirectAttributes ra) {
        try { api.delete(API+"/"+id); ra.addFlashAttribute("success","Procedure deleted."); }
        catch(Exception e){ ra.addFlashAttribute("error", ErrorMessageHelper.friendly(e)); }
        return "redirect:/procedures";
    }
}
