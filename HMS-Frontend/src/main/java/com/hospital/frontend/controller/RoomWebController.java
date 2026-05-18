package com.hospital.frontend.controller;

import com.hospital.frontend.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;


@Controller @RequestMapping("/rooms")
public class RoomWebController {
    @Autowired private ApiService api;
    private static final String API = "/api/rooms";

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page, Model m) {
        m.addAttribute("page", api.getPage(API, null, page)); return "rooms/list";
    }
    @GetMapping("/{id}")
    public String view(@PathVariable int id, Model m) {
        m.addAttribute("item", api.getOne(API + "/" + id));

        // ── Current / Past Occupants (Stays) ──────────────────────────────
        // StayController returns StayDTO with flat fields: stayId, patientSsn, roomNumber, stayStart, stayEnd
        List<Map<String, Object>> allStays = api.getList("/api/stays");
        List<Map<String, Object>> allPatients = api.getList("/api/patients");

        // Build patient lookup: ssn → patient map
        Map<String, Map<String, Object>> patBySsn = new HashMap<>();
        for (Map<String, Object> p : allPatients) {
            Object ssn = p.get("ssn");
            if (ssn != null) patBySsn.put(String.valueOf(ssn), p);
        }

        // Filter by flat roomNumber and enrich with patient name
        List<Map<String, Object>> roomStays = new java.util.ArrayList<>();
        for (Map<String, Object> s : allStays) {
            Object rn = s.get("roomNumber");
            if (rn != null && String.valueOf(rn).equals(String.valueOf(id))) {
                Map<String, Object> enriched = new LinkedHashMap<>(s);
                Object ssn = s.get("patientSsn");
                Map<String, Object> pat = ssn != null ? patBySsn.get(String.valueOf(ssn)) : null;
                enriched.put("patientName", pat != null ? pat.get("name") : "Patient #" + ssn);
                enriched.put("patientSsnResolved", ssn);
                roomStays.add(enriched);
            }
        }
        m.addAttribute("roomStays", roomStays);
        return "rooms/view";
    }
    @GetMapping("/new")
    public String newForm(Model m) {
        m.addAttribute("item", new HashMap<>()); m.addAttribute("isNew", true); return "rooms/form";
    }
    @PostMapping
    public String create(@RequestParam Map<String,String> p, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("roomNumber", Integer.parseInt(p.get("roomNumber"))); d.put("roomType",p.get("roomType"));
            d.put("blockFloor", Integer.parseInt(p.get("blockFloor")));
            d.put("blockCode", Integer.parseInt(p.get("blockCode")));
            d.put("unavailable", Boolean.parseBoolean(p.get("unavailable")));
            api.post(API, d); ra.addFlashAttribute("success","Room created.");
        } catch(Exception e){ ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/rooms";
    }
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id, Model m) {
        m.addAttribute("item", api.getOne(API+"/"+id)); m.addAttribute("isNew",false); return "rooms/form";
    }
    @PostMapping("/{id}/update")
    public String update(@PathVariable int id, @RequestParam Map<String,String> p, RedirectAttributes ra) {
        try {
            Map<String,Object> d = new HashMap<>();
            d.put("roomNumber",id); d.put("roomType",p.get("roomType"));
            d.put("blockFloor", Integer.parseInt(p.get("blockFloor")));
            d.put("blockCode", Integer.parseInt(p.get("blockCode")));
            d.put("unavailable", Boolean.parseBoolean(p.get("unavailable")));
            api.put(API+"/"+id, d); ra.addFlashAttribute("success","Updated.");
        } catch(Exception e){ ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/rooms/"+id;
    }
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id, RedirectAttributes ra) {
        try { api.delete(API+"/"+id); ra.addFlashAttribute("success","Deleted."); }
        catch(Exception e){ ra.addFlashAttribute("error","Cannot delete: "+e.getMessage()); }
        return "redirect:/rooms";
    }
}
