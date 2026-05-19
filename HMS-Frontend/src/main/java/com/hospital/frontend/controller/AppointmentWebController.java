package com.hospital.frontend.controller;

import com.hospital.frontend.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller @RequestMapping("/appointments")
public class AppointmentWebController {
    @Autowired private ApiService api;
    private static final String API         = "/api/appointments";
    private static final String PATIENT_API = "/api/patients";
    private static final String PHY_API     = "/api/physicians";
    private static final String NURSE_API   = "/api/nurses";

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page, Model m) {
        m.addAttribute("page", api.getPage(API, null, page)); return "appointments/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable int id, Model m) {
        m.addAttribute("item", api.getOne(API+"/"+id)); return "appointments/view";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        if (!m.containsAttribute("item")) m.addAttribute("item", new HashMap<>());
        m.addAttribute("isNew", true);
        loadDropdowns(m);
        return "appointments/form";
    }

    @PostMapping
    public String create(@RequestParam Map<String,String> p, Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = buildPayload(null, p);
            api.post(API, d);
            ra.addFlashAttribute("success", "Appointment created successfully.");
            return "redirect:/appointments";
        } catch (Exception e) {
            m.addAttribute("error", e.getMessage());
            m.addAttribute("item", p);
            m.addAttribute("isNew", true);
            loadDropdowns(m);
            return "appointments/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id, Model m) {
        m.addAttribute("item", api.getOne(API+"/"+id));
        m.addAttribute("isNew", false);
        loadDropdowns(m);
        return "appointments/form";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable int id, @RequestParam Map<String,String> p,
                         Model m, RedirectAttributes ra) {
        try {
            Map<String,Object> d = buildPayload(id, p);
            api.put(API+"/"+id, d);
            ra.addFlashAttribute("success", "Appointment updated successfully.");
            return "redirect:/appointments/"+id;
        } catch (Exception e) {
            Map<String,Object> item = new HashMap<>(p);
            item.put("appointmentId", id);
            m.addAttribute("error", e.getMessage());
            m.addAttribute("item", item);
            m.addAttribute("isNew", false);
            loadDropdowns(m);
            return "appointments/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id, RedirectAttributes ra) {
        try { api.delete(API+"/"+id); ra.addFlashAttribute("success","Appointment deleted."); }
        catch(Exception e){ ra.addFlashAttribute("error","Cannot delete: "+e.getMessage()); }
        return "redirect:/appointments";
    }

    // -----------------------------------------------------------------------
    //  Helpers
    // -----------------------------------------------------------------------

    /** Loads patients, physicians, and nurses lists into model for dropdowns. */
    private void loadDropdowns(Model m) {
        m.addAttribute("patients",   api.getList(PATIENT_API));
        m.addAttribute("physicians", api.getList(PHY_API));
        m.addAttribute("nurses",     api.getList(NURSE_API));
    }

    /**
     * Builds the JSON payload for the backend.
     * Combines separate startDate+startTime and endDate+endTime into ISO datetime strings.
     */
    private Map<String,Object> buildPayload(Integer idOverride, Map<String,String> p) {
        Map<String,Object> d = new HashMap<>();
        if (idOverride != null) {
            d.put("appointmentId", idOverride);
        } else if (p.get("appointmentId") != null && !p.get("appointmentId").isBlank()) {
            d.put("appointmentId", Integer.parseInt(p.get("appointmentId")));
        }
        d.put("examinationRoom", p.get("examinationRoom"));

        // Combine date + time fields into ISO datetime string
        String startDate = p.get("startDate"); String startTime = p.get("startTime");
        String endDate   = p.get("endDate");   String endTime   = p.get("endTime");
        if (startDate != null && !startDate.isBlank() && startTime != null && !startTime.isBlank())
            d.put("start", startDate + "T" + startTime);
        if (endDate != null && !endDate.isBlank() && endTime != null && !endTime.isBlank())
            d.put("end", endDate + "T" + endTime);

        d.put("patientSsn",  Integer.parseInt(p.get("patientSsn")));
        d.put("physicianId", Integer.parseInt(p.get("physicianId")));

        String nurseId = p.get("prepNurseId");
        if (nurseId != null && !nurseId.isBlank())
            d.put("prepNurseId", Integer.parseInt(nurseId));

        return d;
    }
}
