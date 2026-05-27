package edu.cit.basalo.vigilo.features.system;

import edu.cit.basalo.vigilo.features.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HolidayStatusService {

    private final UserService userService;
    private final RestTemplate restTemplate = new RestTemplate();

    public HolidayStatusService(UserService userService) {
        this.userService = userService;
    }

    public Map<String, Object> getHolidayStatus(String requesterEmail) {
        userService.requireExistingUser(requesterEmail);

        LocalDate today = LocalDate.now();
        int year = today.getYear();
        String url = "https://date.nager.at/api/v3/PublicHolidays/" + year + "/PH";

        @SuppressWarnings("unchecked")
        Map<String, Object>[] holidays = restTemplate.getForObject(url, Map[].class);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("countryCode", "PH");
        response.put("date", today.toString());

        if (holidays == null || holidays.length == 0) {
            response.put("isHoliday", false);
            response.put("message", "Normal Access Day (PH)");
            return response;
        }

        java.util.List<Map<String, Object>> futureHolidays = Arrays.stream(holidays)
            .filter(holiday -> String.valueOf(holiday.get("date")).compareTo(today.toString()) > 0)
            .sorted(Comparator.comparing(holiday -> String.valueOf(holiday.get("date"))))
            .collect(java.util.stream.Collectors.toList());

        java.util.List<Map<String, Object>> allHolidays = Arrays.stream(holidays)
            .sorted(Comparator.comparing(holiday -> String.valueOf(holiday.get("date"))))
            .collect(java.util.stream.Collectors.toList());

        response.put("allHolidays", allHolidays);

        Map<String, Object> todayHoliday = Arrays.stream(holidays)
            .filter(holiday -> today.toString().equals(String.valueOf(holiday.get("date"))))
            .findFirst()
            .orElse(null);

        if (todayHoliday != null) {
            response.put("isHoliday", true);
            response.put("holidayName", String.valueOf(todayHoliday.get("name")));
            response.put("holidayDate", String.valueOf(todayHoliday.get("date")));
            response.put("message", "Restricted Access Day (PH): " + todayHoliday.get("name"));
            return response;
        }

        response.put("isHoliday", false);
        if (!futureHolidays.isEmpty()) {
            Map<String, Object> nextHoliday = futureHolidays.get(0);
            response.put("holidayName", String.valueOf(nextHoliday.get("name")));
            response.put("holidayDate", String.valueOf(nextHoliday.get("date")));
            response.put("message", "Upcoming Holiday (Philippines): " + nextHoliday.get("name") + " on " + nextHoliday.get("date"));
        } else {
            response.put("message", "No upcoming public holidays in PH for the rest of the year.");
        }
        return response;
    }
}
