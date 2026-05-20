package com.hospital.frontend.controller;

import com.hospital.frontend.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
import java.time.LocalDateTime;

@Controller @RequestMapping("/appointments")
public class AppointmentWebController {
    @Autowired private ApiService api;
    private static final String API         = "/api/appointments";
    private static final String PATIENT_API = "/api/patients";
    private static final String PHY_API     = "/api/physicians";
    private static final String NURSE_API   = "/api/nurses";
    private static final String ROOM_API    = "/api/rooms";

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
            validateRoomAvailable(p.get("examinationRoom"));
            validateNotInPast(p);
            validateNoConflicts(null, p);
            Map<String,Object> d = buildPayload(null, p);
            api.post(API, d);
            markRoomUnavailable(p.get("examinationRoom")); // mark room booked
            ra.addFlashAttribute("success", "Appointment created successfully.");
            return "redirect:/appointments";
        } catch (Exception e) {
            m.addAttribute("error", ErrorMessageHelper.friendly(e));
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
            validateRoomAvailable(p.get("examinationRoom"));
            validateNotInPast(p);
            validateNoConflicts(id, p);
            Map<String,Object> d = buildPayload(id, p);
            api.put(API+"/"+id, d);
            markRoomUnavailable(p.get("examinationRoom")); // mark room booked
            ra.addFlashAttribute("success", "Appointment updated successfully.");
            return "redirect:/appointments/"+id;
        } catch (Exception e) {
            Map<String,Object> item = new HashMap<>(p);
            item.put("appointmentId", id);
            m.addAttribute("error", ErrorMessageHelper.friendly(e));
            m.addAttribute("item", item);
            m.addAttribute("isNew", false);
            loadDropdowns(m);
            return "appointments/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id, RedirectAttributes ra) {
        try { api.delete(API+"/"+id); ra.addFlashAttribute("success","Appointment deleted."); }
        catch(Exception e){ ra.addFlashAttribute("error", ErrorMessageHelper.friendly(e)); }
        return "redirect:/appointments";
    }

    // -----------------------------------------------------------------------
    //  Helpers
    // -----------------------------------------------------------------------

    /** Loads patients, physicians, nurses (registered only), and rooms lists into model for dropdowns. */
    private void loadDropdowns(Model m) {
        m.addAttribute("patients",   api.getList(PATIENT_API));
        m.addAttribute("physicians", api.getList(PHY_API));

        // Only show registered nurses in the Prep Nurse dropdown
        List<Map<String,Object>> allNurses = api.getList(NURSE_API);
        List<Map<String,Object>> registeredNurses = new ArrayList<>();
        for (Map<String,Object> nurse : allNurses) {
            Object reg = nurse.get("registered");
            if (Boolean.TRUE.equals(reg) || "true".equalsIgnoreCase(String.valueOf(reg))) {
                registeredNurses.add(nurse);
            }
        }
        m.addAttribute("nurses", registeredNurses);

        // Only show available rooms in the Examination Room dropdown
        List<Map<String,Object>> allRooms = api.getList(ROOM_API);
        List<Map<String,Object>> availableRooms = new ArrayList<>();
        for (Map<String,Object> room : allRooms) {
            Object unavailable = room.get("unavailable");
            if (!Boolean.TRUE.equals(unavailable) && !"true".equalsIgnoreCase(String.valueOf(unavailable))) {
                availableRooms.add(room);
            }
        }
        m.addAttribute("rooms", availableRooms);
    }

    /**
     * Fetches the room by its roomNumber and throws if it is marked unavailable.
     * @param roomNumberStr the room number as a string (from form param)
     */
    private void validateRoomAvailable(String roomNumberStr) {
        if (roomNumberStr == null || roomNumberStr.isBlank()) return;
        try {
            Map<String,Object> room = api.getOne(ROOM_API + "/" + roomNumberStr.trim());
            if (room != null) {
                Object unavailable = room.get("unavailable");
                if (Boolean.TRUE.equals(unavailable) || "true".equalsIgnoreCase(String.valueOf(unavailable))) {
                    throw new IllegalStateException(
                            "Room " + roomNumberStr + " is currently unavailable and cannot be used for an appointment.");
                }
            }
        } catch (IllegalStateException ex) {
            throw ex;   // re-throw our own validation exception
        } catch (Exception ex) {
            // If room lookup fails for any other reason, skip validation gracefully
        }
    }

    /**
     * Fetches the room by its roomNumber and flips unavailable=true via PUT.
     * Failure is silent — the appointment is already saved.
     */
    private void markRoomUnavailable(String roomNumberStr) {
        if (roomNumberStr == null || roomNumberStr.isBlank()) return;
        try {
            Map<String,Object> room = api.getOne(ROOM_API + "/" + roomNumberStr.trim());
            if (room != null) {
                Map<String,Object> updated = new HashMap<>(room);
                updated.put("unavailable", true);
                api.put(ROOM_API + "/" + roomNumberStr.trim(), updated);
            }
        } catch (Exception ex) {
            // Non-critical: appointment already saved, room status update failed silently
        }
    }

    /**
     * Validates that the appointment start date/time is not in the past.
     * Throws IllegalStateException if the submitted start is before LocalDateTime.now().
     */
    private void validateNotInPast(Map<String,String> p) {
        String startDate = p.get("startDate");
        String startTime = p.get("startTime");
        if (startDate == null || startDate.isBlank() || startTime == null || startTime.isBlank()) return;
        try {
            LocalDateTime submitted = LocalDateTime.parse(startDate + "T" + startTime);
            if (submitted.isBefore(LocalDateTime.now())) {
                throw new IllegalStateException(
                    "Appointment start date/time (" + startDate + " " + startTime +
                    ") cannot be in the past. Please select today or a future date and time.");
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            // Parsing failed — backend will validate further
        }
    }

    /**
     * Checks all existing appointments for scheduling conflicts at the same time.
     * Throws IllegalStateException if the same patient, physician, or nurse is
     * already booked in an overlapping time window.
     *
     * @param excludeId appointment ID to skip (the one being updated); null for new
     * @param p         raw form params (startDate, startTime, endDate, endTime,
     *                  patientSsn, physicianId, prepNurseId)
     */
    private void validateNoConflicts(Integer excludeId, Map<String,String> p) {
        String newStart    = p.get("startDate") + "T" + p.get("startTime");
        String newEnd      = p.get("endDate")   + "T" + p.get("endTime");
        String newPatient  = p.get("patientSsn")  != null ? p.get("patientSsn").trim()  : "";
        String newPhysician= p.get("physicianId") != null ? p.get("physicianId").trim() : "";
        String newNurse    = p.get("prepNurseId") != null ? p.get("prepNurseId").trim() : "";

        List<Map<String,Object>> all = api.getList(API);
        if (all == null) return;

        for (Map<String,Object> existing : all) {
            // Skip the appointment being edited
            Object existingId = existing.get("appointmentId");
            if (excludeId != null && existingId != null
                    && String.valueOf(existingId).equals(String.valueOf(excludeId))) {
                continue;
            }

            // Extract existing time window
            String exStart = existing.get("start") != null ? existing.get("start").toString() : "";
            String exEnd   = existing.get("end")   != null ? existing.get("end").toString()   : "";
            if (exStart.isEmpty() || exEnd.isEmpty()) continue;

            // Overlap: newStart < exEnd  AND  exStart < newEnd
            boolean overlaps = newStart.compareTo(exEnd) < 0 && exStart.compareTo(newEnd) < 0;
            if (!overlaps) continue;

            // Check patient conflict
            Object exPatient = existing.get("patientSsn");
            if (!newPatient.isEmpty() && exPatient != null
                    && newPatient.equals(String.valueOf(exPatient))) {
                throw new IllegalStateException(
                    "Scheduling conflict: Patient (SSN " + newPatient +
                    ") already has an appointment overlapping " + newStart + " – " + newEnd + ".");
            }

            // Check physician conflict
            Object exPhysician = existing.get("physicianId");
            if (!newPhysician.isEmpty() && exPhysician != null
                    && newPhysician.equals(String.valueOf(exPhysician))) {
                throw new IllegalStateException(
                    "Scheduling conflict: Physician (ID " + newPhysician +
                    ") is already booked in an overlapping time slot " + newStart + " – " + newEnd + ".");
            }

            // Check nurse conflict (only if a nurse was selected)
            Object exNurse = existing.get("prepNurseId");
            if (!newNurse.isEmpty() && exNurse != null
                    && newNurse.equals(String.valueOf(exNurse))) {
                throw new IllegalStateException(
                    "Scheduling conflict: Prep Nurse (ID " + newNurse +
                    ") is already assigned to another appointment overlapping " + newStart + " – " + newEnd + ".");
            }
        }
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