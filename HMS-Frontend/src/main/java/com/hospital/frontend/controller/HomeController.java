package com.hospital.frontend.controller;

import com.hospital.frontend.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired private ApiService api;

    @GetMapping("/")
    public String home(Model model) {
        try { model.addAttribute("physicianCount", api.getPage("/api/physicians", null, 0).getTotalElements()); }
        catch (Exception e) { model.addAttribute("physicianCount", "–"); }

        try { model.addAttribute("patientCount", api.getPage("/api/patients", null, 0).getTotalElements()); }
        catch (Exception e) { model.addAttribute("patientCount", "–"); }

        try { model.addAttribute("nurseCount", api.getPage("/api/nurses", null, 0).getTotalElements()); }
        catch (Exception e) { model.addAttribute("nurseCount", "–"); }

        try { model.addAttribute("deptCount", api.getPage("/api/departments", null, 0).getTotalElements()); }
        catch (Exception e) { model.addAttribute("deptCount", "–"); }

        try { model.addAttribute("apptCount", api.getPage("/api/appointments", null, 0).getTotalElements()); }
        catch (Exception e) { model.addAttribute("apptCount", "–"); }


        try { model.addAttribute("procCount", api.getPage("/api/procedures", null, 0).getTotalElements()); }
        catch (Exception e) { model.addAttribute("procCount", "–"); }

        try { model.addAttribute("roomCount", api.getPage("/api/rooms", null, 0).getTotalElements()); }
        catch (Exception e) { model.addAttribute("roomCount", "–"); }

        return "index";
    }
}
