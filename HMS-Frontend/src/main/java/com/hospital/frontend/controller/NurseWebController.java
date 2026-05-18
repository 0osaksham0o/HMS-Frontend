package com.hospital.frontend.controller;

import com.hospital.frontend.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller @RequestMapping("/nurses")
public class NurseWebController {
    @Autowired private ApiService api;
    private static final String API = "/api/nurses";

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="") String search, Model m) {
        m.addAttribute("page", search.isBlank()
                ? api.getPage(API, "nurses", page)
                : api.getPageSearch(API, search, page));
        m.addAttribute("search", search);
        return "nurses/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable int id, Model m) {
        Map<String, Object> nurse = api.getOne(API + "/" + id);
        m.addAttribute("item", nurse);
        String nurseId = String.valueOf(id);

        // ── On-Call Schedule (/api/oncalls → OnCallDTO flat format) ──────────
        List<Map<String, Object>> allOnCalls = api.getList("/api/oncalls");
        List<Map<String, Object>> onCalls = new ArrayList<>();
        for (Map<String, Object> oc : allOnCalls) {
            Object nid = oc.get("nurseId");
            // Also handle HATEOAS nested id just in case
            if (nid == null) { Object idObj = oc.get("id"); if (idObj instanceof Map) nid = ((Map<?,?>)idObj).get("nurseId"); }
            if (nid != null && String.valueOf(nid).equals(nurseId)) onCalls.add(oc);
        }
        m.addAttribute("onCalls", onCalls);

        // ── Prep Nurse Appointments (/api/appointments → Appointment entity) ──
        List<Map<String, Object>> allAppts = api.getList("/api/appointments");
        List<Map<String, Object>> prepAppts = new ArrayList<>();
        for (Map<String, Object> a : allAppts) {
            boolean matches = false;
            // Case 1: prepNurse is a nested Map with employeeId
            Object pn = a.get("prepNurse");
            if (pn instanceof Map) {
                Object eid = ((Map<?,?>) pn).get("employeeId");
                if (eid != null && String.valueOf(eid).equals(nurseId)) matches = true;
            }
            // Case 2: flat prepNurseId field (from projection)
            if (!matches) {
                Object flat = a.get("prepNurseId");
                if (flat != null && String.valueOf(flat).equals(nurseId)) matches = true;
            }
            if (matches) prepAppts.add(a);
        }
        m.addAttribute("prepAppts", prepAppts);

        // ── Assisted Procedures (/api/undergoes → Undergoes entity) ──────────
        List<Map<String, Object>> allUndergoes = api.getList("/api/undergoes");
        List<Map<String, Object>> assistedProcs = new ArrayList<>();
        for (Map<String, Object> u : allUndergoes) {
            Object an = u.get("assistingNurse");
            if (an instanceof Map) {
                Object eid = ((Map<?,?>) an).get("employeeId");
                if (eid != null && String.valueOf(eid).equals(nurseId)) assistedProcs.add(u);
            }
        }
        m.addAttribute("assistedProcs", assistedProcs);

        return "nurses/view";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        if (!m.containsAttribute("item")) m.addAttribute("item", new HashMap<>());
        m.addAttribute("isNew", true);
        return "nurses/form";
    }

    @PostMapping
    public String create(@RequestParam Map<String,String> p, Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("employeeId", Integer.parseInt(p.get("employeeId")));
            d.put("name", p.get("name"));
            d.put("position", p.get("position"));
            d.put("registered", Boolean.parseBoolean(p.get("registered")));
            d.put("ssn", Integer.parseInt(p.get("ssn")));
            api.post(API, d);
            ra.addFlashAttribute("success", "Nurse created successfully.");
            return "redirect:/nurses";
        } catch (Exception e) {
            m.addAttribute("error", e.getMessage());
            m.addAttribute("item", p);
            m.addAttribute("isNew", true);
            return "nurses/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id, Model m) {
        m.addAttribute("item", api.getOne(API+"/"+id)); m.addAttribute("isNew",false); return "nurses/form";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable int id, @RequestParam Map<String,String> p,
                         Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("employeeId", id); d.put("name", p.get("name"));
            d.put("position", p.get("position"));
            d.put("registered", Boolean.parseBoolean(p.get("registered")));
            d.put("ssn", Integer.parseInt(p.get("ssn")));
            api.put(API+"/"+id, d);
            ra.addFlashAttribute("success", "Nurse updated successfully.");
            return "redirect:/nurses/"+id;
        } catch (Exception e) {
            Map<String,Object> item = new HashMap<>(p);
            item.put("employeeId", id);
            m.addAttribute("error", e.getMessage());
            m.addAttribute("item", item);
            m.addAttribute("isNew", false);
            return "nurses/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id, RedirectAttributes ra) {
        try { api.delete(API+"/"+id); ra.addFlashAttribute("success","Nurse deleted."); }
        catch(Exception e){ ra.addFlashAttribute("error","Cannot delete: "+e.getMessage()); }
        return "redirect:/nurses";
    }
}
