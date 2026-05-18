package com.hospital.frontend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/medications")
public class MedicationWebController {

    @GetMapping
    public String list() { return "redirect:/"; }

    @GetMapping("/{id}")
    public String view(@PathVariable int id) { return "redirect:/"; }

    @GetMapping("/new")
    public String newForm() { return "redirect:/"; }

    @PostMapping
    public String create() { return "redirect:/"; }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable int id) { return "redirect:/"; }

    @PostMapping("/{id}/update")
    public String update(@PathVariable int id) { return "redirect:/"; }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id) { return "redirect:/"; }
}
