package com.hospital.frontend.controller;

import com.hospital.frontend.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller @RequestMapping("/patients")
public class PatientWebController {
    @Autowired private ApiService api;
    private static final String API = "/api/patients";

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="") String search, Model m) {
        m.addAttribute("page", search.isBlank()
                ? api.getPage(API, null, page)
                : api.getPageSearch(API, search, page));
        m.addAttribute("search", search);
        return "patients/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable int id, Model m) {
        Map<String, Object> patient = api.getOne(API + "/" + id);
        m.addAttribute("item", patient);

        // Fetch appointments for this patient
        List<Map<String, Object>> allAppointments = api.getList("/api/appointments");
        List<Map<String, Object>> patientAppointments = allAppointments.stream()
                .filter(a -> {
                    Object ssn = a.get("patientSsn");
                    if (ssn == null) {
                        // try nested patient object
                        Object p = a.get("patient");
                        if (p instanceof Map) ssn = ((Map<?,?>)p).get("ssn");
                    }
                    return ssn != null && String.valueOf(ssn).equals(String.valueOf(id));
                })
                .collect(java.util.stream.Collectors.toList());
        m.addAttribute("appointments", patientAppointments);

        return "patients/view";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        if (!m.containsAttribute("item")) m.addAttribute("item", new HashMap<>());
        m.addAttribute("isNew", true);
        m.addAttribute("physicians", api.getList("/api/physicians"));
        return "patients/form";
    }

    @PostMapping
    public String create(@RequestParam Map<String,String> p, Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("ssn", Integer.parseInt(p.get("ssn")));
            d.put("name", p.get("name"));
            d.put("address", p.get("address"));
            d.put("phone", p.get("phone"));
            String insId = p.get("insuranceId");
            if (insId != null && !insId.isBlank()) d.put("insuranceId", Integer.parseInt(insId));
            String pcpId = p.get("pcpId");
            if (pcpId != null && !pcpId.isBlank()) d.put("pcpId", Integer.parseInt(pcpId));
            api.post(API, d);
            ra.addFlashAttribute("success", "Patient created successfully.");
            return "redirect:/patients";
        } catch (Exception e) {
            m.addAttribute("error", ErrorMessageHelper.friendly(e));
            m.addAttribute("item", p);
            m.addAttribute("isNew", true);
            m.addAttribute("physicians", api.getList("/api/physicians"));
            return "patients/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id, Model m) {
        m.addAttribute("item", api.getOne(API+"/"+id));
        m.addAttribute("isNew", false);
        m.addAttribute("physicians", api.getList("/api/physicians"));
        return "patients/form";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable int id, @RequestParam Map<String,String> p,
                         Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("ssn", id); d.put("name", p.get("name"));
            d.put("address", p.get("address")); d.put("phone", p.get("phone"));
            String insId = p.get("insuranceId");
            if (insId != null && !insId.isBlank()) d.put("insuranceId", Integer.parseInt(insId));
            String pcpId = p.get("pcpId");
            if (pcpId != null && !pcpId.isBlank()) d.put("pcpId", Integer.parseInt(pcpId));
            api.put(API+"/"+id, d);
            ra.addFlashAttribute("success", "Patient updated successfully.");
            return "redirect:/patients/"+id;
        } catch (Exception e) {
            Map<String,Object> item = new HashMap<>(p);
            item.put("ssn", id);
            m.addAttribute("error", ErrorMessageHelper.friendly(e));
            m.addAttribute("item", item);
            m.addAttribute("isNew", false);
            m.addAttribute("physicians", api.getList("/api/physicians"));
            return "patients/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id, RedirectAttributes ra) {
        try { api.delete(API+"/"+id); ra.addFlashAttribute("success","Patient deleted."); }
        catch(Exception e){ ra.addFlashAttribute("error", ErrorMessageHelper.friendly(e)); }
        return "redirect:/patients";
    }
}
